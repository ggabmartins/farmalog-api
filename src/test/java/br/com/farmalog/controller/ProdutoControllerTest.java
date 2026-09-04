package br.com.farmalog.controller;

import br.com.farmalog.dto.ProdutoResponse;
import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.service.ProdutoService;
import br.com.farmalog.validation.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProdutoController.class)
@Import(GlobalExceptionHandler.class)
class ProdutoControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProdutoService service;

	@Test
	void criar_comCorpoInvalido_retorna400ComCampos() throws Exception {
		mockMvc.perform(post("/api/v1/produtos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.campos").isArray());
	}

	@Test
	void criar_valido_retorna201ComLocation() throws Exception {
		var response = new ProdutoResponse(1L, "Dipirona", "Dipirona sódica", "EMS",
				"7891234567890", new BigDecimal("12.90"), ExigenciaReceita.ISENTO, 10, true);
		when(service.criar(any())).thenReturn(response);

		mockMvc.perform(post("/api/v1/produtos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Dipirona",
								  "principioAtivo": "Dipirona sódica",
								  "fabricante": "EMS",
								  "codigoBarras": "7891234567890",
								  "precoVenda": 12.90,
								  "exigencia": "ISENTO",
								  "estoqueMinimo": 10
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/produtos/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void desativar_retorna204() throws Exception {
		mockMvc.perform(delete("/api/v1/produtos/1"))
				.andExpect(status().isNoContent());
	}
}
