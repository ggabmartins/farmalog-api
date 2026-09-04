package br.com.farmalog.controller;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	private static RequestPostProcessor gerente() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"));
	}

	private static final String CORPO = """
			{
			  "nome": "Amoxicilina 500mg",
			  "principioAtivo": "Amoxicilina",
			  "fabricante": "Medley",
			  "codigoBarras": "7899999999999",
			  "precoVenda": 29.90,
			  "exigencia": "RECEITA_SIMPLES",
			  "estoqueMinimo": 5
			}
			""";

	@Test
	void fluxoCompleto_criar_buscar_atualizar_listar_desativar() throws Exception {
		String location = mockMvc.perform(post("/api/v1/produtos").with(gerente())
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location).with(gerente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Amoxicilina 500mg"))
				.andExpect(jsonPath("$.ativo").value(true));

		String atualizado = CORPO.replace("29.90", "34.50");
		mockMvc.perform(put(location).with(gerente()).contentType(MediaType.APPLICATION_JSON).content(atualizado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precoVenda").value(34.50));

		mockMvc.perform(get("/api/v1/produtos").with(gerente()).param("nome", "amoxicilina"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].codigoBarras").value("7899999999999"));

		mockMvc.perform(delete(location).with(gerente()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/produtos").with(gerente()).param("nome", "amoxicilina"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void criar_comCodigoDeBarrasDuplicado_retorna409() throws Exception {
		mockMvc.perform(post("/api/v1/produtos").with(gerente())
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/produtos").with(gerente())
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
