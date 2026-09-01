package br.com.farmalog.produto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
		String location = mockMvc.perform(post("/api/v1/produtos")
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Amoxicilina 500mg"))
				.andExpect(jsonPath("$.ativo").value(true));

		String atualizado = CORPO.replace("29.90", "34.50");
		mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(atualizado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precoVenda").value(34.50));

		mockMvc.perform(get("/api/v1/produtos").param("nome", "amoxicilina"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].codigoBarras").value("7899999999999"));

		mockMvc.perform(delete(location))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/produtos").param("nome", "amoxicilina"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void criar_comCodigoDeBarrasDuplicado_retorna409() throws Exception {
		mockMvc.perform(post("/api/v1/produtos")
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/produtos")
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
