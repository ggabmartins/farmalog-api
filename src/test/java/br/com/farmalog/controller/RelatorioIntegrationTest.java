package br.com.farmalog.controller;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
class RelatorioIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProdutoRepository produtoRepository;

	@Autowired
	LoteRepository loteRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static RequestPostProcessor gerente() {
		return jwt().jwt(j -> j.subject("gerente@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_GERENTE"));
	}

	private static RequestPostProcessor farmaceutico() {
		return jwt().jwt(j -> j.subject("farmaceutico@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"));
	}

	private static RequestPostProcessor atendente() {
		return jwt().jwt(j -> j.subject("atendente@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
	}

	private Produto produtoComEstoque(String nome, String preco, int saldo) {
		Produto produto = produtoRepository.save(Produto.builder()
				.nome(nome).principioAtivo("X").fabricante("ACME")
				.codigoBarras("792" + System.nanoTime() % 10000000000L)
				.precoVenda(new BigDecimal(preco)).exigencia(ExigenciaReceita.ISENTO)
				.estoqueMinimo(1).ativo(true).build());
		loteRepository.save(Lote.builder()
				.produto(produto).codigo("L" + System.nanoTime()).quantidadeAtual(saldo)
				.dataValidade(LocalDate.now().plusMonths(6))
				.precoCusto(new BigDecimal("1.00")).dataEntrada(LocalDate.now().minusDays(1))
				.build());
		return produto;
	}

	private Long vender(Produto produto, int quantidade) throws Exception {
		String corpo = """
				{ "itens": [ { "produtoId": %d, "quantidade": %d } ] }
				""".formatted(produto.getId(), quantidade);

		String resposta = mockMvc.perform(post("/api/v1/vendas").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(resposta).get("id").asLong();
	}

	private void cancelar(Long vendaId) throws Exception {
		mockMvc.perform(post("/api/v1/vendas/" + vendaId + "/cancelamento").with(farmaceutico()))
				.andExpect(status().isNoContent());
	}

	private JsonNode relatorioDeVendas() throws Exception {
		String resposta = mockMvc.perform(get("/api/v1/relatorios/vendas").with(gerente()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(resposta);
	}

	private JsonNode ranking(String limite) throws Exception {
		var requisicao = get("/api/v1/relatorios/produtos-mais-vendidos").with(gerente());
		if (limite != null) {
			requisicao.param("limite", limite);
		}
		String resposta = mockMvc.perform(requisicao)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(resposta);
	}

	private static int posicao(JsonNode ranking, Long produtoId) {
		for (int i = 0; i < ranking.size(); i++) {
			if (ranking.get(i).get("produtoId").asLong() == produtoId) {
				return i;
			}
		}
		return -1;
	}

	@Test
	void vendas_somaConcluidasEMostraCanceladasSeparadas() throws Exception {
		JsonNode antes = relatorioDeVendas();

		Produto produto = produtoComEstoque("Relatorio Dipirona", "10.00", 100);
		vender(produto, 1);
		vender(produto, 2);
		cancelar(vender(produto, 3));

		JsonNode depois = relatorioDeVendas();

		assertThat(depois.get("totalVendas").asLong() - antes.get("totalVendas").asLong()).isEqualTo(2);
		assertThat(depois.get("faturamento").decimalValue().subtract(antes.get("faturamento").decimalValue()))
				.isEqualByComparingTo("30.00");
		assertThat(depois.get("totalCanceladas").asLong() - antes.get("totalCanceladas").asLong()).isEqualTo(1);
		assertThat(depois.get("valorCancelado").decimalValue().subtract(antes.get("valorCancelado").decimalValue()))
				.isEqualByComparingTo("30.00");
	}

	@Test
	void maisVendidos_ordenaPorQuantidade_eIgnoraVendasCanceladas() throws Exception {
		Produto a = produtoComEstoque("Ranking A", "5.00", 1000);
		Produto b = produtoComEstoque("Ranking B", "5.00", 1000);
		vender(a, 200);
		cancelar(vender(a, 100));
		vender(b, 250);

		JsonNode ranking = ranking("20");

		int posicaoA = posicao(ranking, a.getId());
		int posicaoB = posicao(ranking, b.getId());
		assertThat(posicaoA).isGreaterThanOrEqualTo(0);
		assertThat(posicaoB).isGreaterThanOrEqualTo(0).isLessThan(posicaoA);
		assertThat(ranking.get(posicaoA).get("quantidadeVendida").asLong()).isEqualTo(200);
		assertThat(ranking.get(posicaoB).get("quantidadeVendida").asLong()).isEqualTo(250);
	}

	@Test
	void maisVendidos_respeitaOLimite_eOTetoDeVinte() throws Exception {
		for (int i = 0; i < 21; i++) {
			vender(produtoComEstoque("Limite " + i, "5.00", 10), 1);
		}

		assertThat(ranking(null)).hasSize(10);
		assertThat(ranking("3")).hasSize(3);
		assertThat(ranking("500")).hasSize(20);
		assertThat(ranking("0")).hasSize(1);
	}

	@Test
	void relatorios_soGerenteAcessa() throws Exception {
		for (String rota : new String[]{"/api/v1/relatorios/vendas", "/api/v1/relatorios/produtos-mais-vendidos"}) {
			mockMvc.perform(get(rota).with(farmaceutico())).andExpect(status().isForbidden());
			mockMvc.perform(get(rota).with(atendente())).andExpect(status().isForbidden());
			mockMvc.perform(get(rota)).andExpect(status().isUnauthorized());
			mockMvc.perform(get(rota).with(gerente())).andExpect(status().isOk());
		}
	}

	@Test
	void vendas_periodoSemNenhumaVenda_devolveZeros() throws Exception {
		mockMvc.perform(get("/api/v1/relatorios/vendas")
						.param("inicio", "2099-01-01").param("fim", "2099-01-31").with(gerente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalVendas").value(0))
				.andExpect(jsonPath("$.faturamento").value(0.00))
				.andExpect(jsonPath("$.ticketMedio").value(0.00))
				.andExpect(jsonPath("$.totalCanceladas").value(0))
				.andExpect(jsonPath("$.valorCancelado").value(0.00));
	}
}
