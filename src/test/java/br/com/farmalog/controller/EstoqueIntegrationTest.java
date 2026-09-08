package br.com.farmalog.controller;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EstoqueIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProdutoRepository produtoRepository;

	@Autowired
	LoteRepository loteRepository;

	private static RequestPostProcessor farmaceutico() {
		return jwt().jwt(j -> j.subject("farmaceutico@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"));
	}

	private static RequestPostProcessor atendente() {
		return jwt().jwt(j -> j.subject("atendente@farmalog.dev"))
				.authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
	}

	private Produto produto(int estoqueMinimo) {
		return produtoRepository.save(Produto.builder()
				.nome("Amoxicilina 500mg").principioAtivo("Amoxicilina").fabricante("Medley")
				.codigoBarras("789" + System.nanoTime() % 100000000000L)
				.precoVenda(new BigDecimal("29.90"))
				.exigencia(ExigenciaReceita.RECEITA_SIMPLES).estoqueMinimo(estoqueMinimo).ativo(true)
				.build());
	}

	private Lote lote(Produto produto, String codigo, LocalDate validade, int saldo) {
		return loteRepository.save(Lote.builder()
				.produto(produto).codigo(codigo).quantidadeAtual(saldo).dataValidade(validade)
				.precoCusto(new BigDecimal("10.00")).dataEntrada(LocalDate.now().minusMonths(1))
				.build());
	}

	@Test
	void entrada_registraSaldoNoLote_eDeixaMovimentacao() throws Exception {
		Produto produto = produto(10);
		String corpo = """
				{ "codigo": "E1", "dataValidade": "%s", "quantidade": 100,
				  "precoCusto": 4.00, "dataEntrada": "%s" }
				""".formatted(LocalDate.now().plusMonths(6), LocalDate.now());

		mockMvc.perform(post("/api/v1/produtos/" + produto.getId() + "/lotes").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantidadeAtual").value(100));

		mockMvc.perform(get("/api/v1/estoque/movimentacoes").param("tipo", "ENTRADA").with(farmaceutico()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].tipo").value("ENTRADA"))
				.andExpect(jsonPath("$.content[0].quantidade").value(100));

		mockMvc.perform(get("/api/v1/produtos/" + produto.getId()).with(farmaceutico()))
				.andExpect(jsonPath("$.quantidadeEmEstoque").value(100));
	}

	@Test
	void atendente_naoPodeRegistrarEntrada_retorna403() throws Exception {
		Produto produto = produto(10);
		String corpo = """
				{ "codigo": "E2", "dataValidade": "%s", "quantidade": 10,
				  "precoCusto": 4.00, "dataEntrada": "%s" }
				""".formatted(LocalDate.now().plusMonths(6), LocalDate.now());

		mockMvc.perform(post("/api/v1/produtos/" + produto.getId() + "/lotes").with(atendente())
						.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
	}

	@Test
	void loteVencido_naoEntraNaDisponibilidadeNemNaContagem() throws Exception {
		Produto produto = produto(50);
		lote(produto, "VENC", LocalDate.now().minusDays(1), 100);
		lote(produto, "OK", LocalDate.now().plusMonths(6), 30);

		mockMvc.perform(get("/api/v1/produtos/" + produto.getId()).with(atendente()))
				.andExpect(jsonPath("$.quantidadeEmEstoque").value(30));

		mockMvc.perform(get("/api/v1/estoque/alertas").with(atendente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.produtosAbaixoDoMinimo[0].produtoId").value(produto.getId()))
				.andExpect(jsonPath("$.produtosAbaixoDoMinimo[0].disponivel").value(30));
	}

	@Test
	void descarte_deLoteNaoVencido_retorna422() throws Exception {
		Lote lote = lote(produto(10), "NV", LocalDate.now().plusMonths(1), 50);

		mockMvc.perform(post("/api/v1/lotes/" + lote.getId() + "/descarte").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"quantidade\": 10, \"observacao\": \"tentativa\" }"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void descarte_semObservacao_retorna400() throws Exception {
		Lote lote = lote(produto(10), "V1", LocalDate.now().minusDays(2), 50);

		mockMvc.perform(post("/api/v1/lotes/" + lote.getId() + "/descarte").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"quantidade\": 10 }"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void descarte_deLoteVencido_reduzOSaldo() throws Exception {
		Lote lote = lote(produto(10), "V2", LocalDate.now().minusDays(2), 50);

		mockMvc.perform(post("/api/v1/lotes/" + lote.getId() + "/descarte").with(farmaceutico())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"quantidade\": 50, \"observacao\": \"vencido, descartado conforme RDC 44/2009\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeAtual").value(0));
	}
}
