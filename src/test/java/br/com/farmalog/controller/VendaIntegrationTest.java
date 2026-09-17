package br.com.farmalog.controller;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import br.com.farmalog.repository.VendaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VendaIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProdutoRepository produtoRepository;

	@Autowired
	LoteRepository loteRepository;

	@Autowired
	VendaRepository vendaRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static RequestPostProcessor atendente() {
		return jwt().jwt(j -> j.subject("atendente@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
	}

	private static RequestPostProcessor farmaceutico() {
		return jwt().jwt(j -> j.subject("farmaceutico@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"));
	}

	private Produto produtoIsento() {
		return produtoRepository.save(Produto.builder()
				.nome("Venda Test").principioAtivo("X").fabricante("ACME")
				.codigoBarras("790" + System.nanoTime() % 10000000000L)
				.precoVenda(new BigDecimal("10.00")).exigencia(ExigenciaReceita.ISENTO)
				.estoqueMinimo(1).ativo(true).build());
	}

	private Produto produtoControlado() {
		return produtoRepository.save(Produto.builder()
				.nome("Controlado Test").principioAtivo("Y").fabricante("ACME")
				.codigoBarras("791" + System.nanoTime() % 10000000000L)
				.precoVenda(new BigDecimal("30.00")).exigencia(ExigenciaReceita.CONTROLADO)
				.estoqueMinimo(1).ativo(true).build());
	}

	private void lote(Produto produto, String codigo, LocalDate validade, int saldo) {
		loteRepository.save(Lote.builder()
				.produto(produto).codigo(codigo).quantidadeAtual(saldo).dataValidade(validade)
				.precoCusto(new BigDecimal("4.00")).dataEntrada(LocalDate.now().minusDays(1))
				.build());
	}

	@Test
	void registrar_baixaPorFefo_eDeixaSaldoCertoEmCadaLote() throws Exception {
		Produto produto = produtoIsento();
		lote(produto, "F1", LocalDate.now().plusMonths(1), 3);
		lote(produto, "F2", LocalDate.now().plusMonths(6), 5);

		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": 4 } ] }
				""".formatted(produto.getId());

		mockMvc.perform(post("/api/v1/vendas").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valorTotal").value(40.00));

		mockMvc.perform(get("/api/v1/produtos/" + produto.getId() + "/lotes").with(atendente()))
				.andExpect(jsonPath("$.content[0].codigo").value("F1"))
				.andExpect(jsonPath("$.content[0].quantidadeAtual").value(0))
				.andExpect(jsonPath("$.content[1].codigo").value("F2"))
				.andExpect(jsonPath("$.content[1].quantidadeAtual").value(4));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void registrar_estoqueInsuficiente_naoGravaNadaDaVenda() throws Exception {
		Produto produto = produtoIsento();
		lote(produto, "A1", LocalDate.now().plusMonths(1), 2);
		long vendasAntes = vendaRepository.count();

		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": 10 } ] }
				""".formatted(produto.getId());

		mockMvc.perform(post("/api/v1/vendas").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnprocessableEntity());

		assertThat(vendaRepository.count()).isEqualTo(vendasAntes);
		mockMvc.perform(get("/api/v1/produtos/" + produto.getId()).with(atendente()))
				.andExpect(jsonPath("$.quantidadeEmEstoque").value(2));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void registrar_semReceitaParaControlado_revertaConsumoJaFeito() throws Exception {
		Produto isento = produtoIsento();
		lote(isento, "I1", LocalDate.now().plusMonths(1), 5);
		Produto controlado = produtoControlado();
		lote(controlado, "C1", LocalDate.now().plusMonths(1), 5);

		String corpo = """
				{ "itens": [
					{ "produtoId": %d, "quantidade": 2 },
					{ "produtoId": %d, "quantidade": 1 }
				  ] }
				""".formatted(isento.getId(), controlado.getId());

		mockMvc.perform(post("/api/v1/vendas").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnprocessableEntity());

		mockMvc.perform(get("/api/v1/produtos/" + isento.getId()).with(farmaceutico()))
				.andExpect(jsonPath("$.quantidadeEmEstoque").value(5));
	}

	@Test
	void atendente_naoPodeVenderControlado_retorna403() throws Exception {
		Produto controlado = produtoControlado();
		lote(controlado, "C2", LocalDate.now().plusMonths(1), 5);

		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": 1 } ],
				  "receita": { "numero": "1", "crmMedico": "CRM1", "dataEmissao": "%s" } }
				""".formatted(controlado.getId(), LocalDate.now());

		mockMvc.perform(post("/api/v1/vendas").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
	}

	@Test
	void cancelar_devolveQuantidadeExataAosLotesDeOrigem() throws Exception {
		Produto produto = produtoIsento();
		lote(produto, "R1", LocalDate.now().plusMonths(1), 3);
		lote(produto, "R2", LocalDate.now().plusMonths(6), 5);

		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": 4 } ] }
				""".formatted(produto.getId());

		String resposta = mockMvc.perform(post("/api/v1/vendas").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andReturn().getResponse().getContentAsString();
		Long vendaId = objectMapper.readTree(resposta).get("id").asLong();

		mockMvc.perform(post("/api/v1/vendas/" + vendaId + "/cancelamento").with(farmaceutico()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/produtos/" + produto.getId() + "/lotes").with(atendente()))
				.andExpect(jsonPath("$.content[0].quantidadeAtual").value(3))
				.andExpect(jsonPath("$.content[1].quantidadeAtual").value(5));
	}

	@Test
	void atendente_naoVeVendaDeOutroOperador() throws Exception {
		Produto produto = produtoIsento();
		lote(produto, "V1", LocalDate.now().plusMonths(1), 5);

		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": 1 } ] }
				""".formatted(produto.getId());

		mockMvc.perform(post("/api/v1/vendas").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/vendas").with(atendente()))
				.andExpect(jsonPath("$.content").isEmpty());
	}
}
