package br.com.farmalog.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ValidationHandlerTest.EndpointsDeTeste.class)
@Import({ValidationHandler.class, ValidationHandlerTest.EndpointsDeTeste.class})
class ValidationHandlerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void responseStatusException_retornaStatusEMensagem() throws Exception {
		mockMvc.perform(get("/teste/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Produto 42 não encontrado"));
	}

	@Test
	void responseStatusException_409() throws Exception {
		mockMvc.perform(get("/teste/duplicado"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void bodyInvalido_retorna400ComListaDeCampos() throws Exception {
		mockMvc.perform(post("/teste/valida")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].field").value("nome"))
				.andExpect(jsonPath("$[0].message").value("não pode estar em branco"));
	}

	@RestController
	static class EndpointsDeTeste {

		@GetMapping("/teste/nao-encontrado")
		void naoEncontrado() {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto 42 não encontrado");
		}

		@GetMapping("/teste/duplicado")
		void duplicado() {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de barras já cadastrado");
		}

		@PostMapping("/teste/valida")
		void valida(@Valid @RequestBody Corpo corpo) {
		}

		record Corpo(@NotBlank(message = "não pode estar em branco") String nome) {
		}
	}
}
