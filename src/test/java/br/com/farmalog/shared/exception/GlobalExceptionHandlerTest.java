package br.com.farmalog.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.EndpointsDeTeste.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.EndpointsDeTeste.class})
class GlobalExceptionHandlerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void recursoNaoEncontrado_retorna404ProblemDetail() throws Exception {
		mockMvc.perform(get("/teste/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Produto 42 não encontrado"))
				.andExpect(jsonPath("$.instance").value("/teste/nao-encontrado"));
	}

	@Test
	void recursoDuplicado_retorna409() throws Exception {
		mockMvc.perform(get("/teste/duplicado"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Conflito com recurso existente"));
	}

	@Test
	void bodyInvalido_retorna400ComListaDeCampos() throws Exception {
		mockMvc.perform(post("/teste/valida")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.instance").value("/teste/valida"))
				.andExpect(jsonPath("$.campos[0].campo").value("nome"))
				.andExpect(jsonPath("$.campos[0].mensagem").value("não pode estar em branco"));
	}

	@Test
	void erroInesperado_retorna500SemStackTrace() throws Exception {
		mockMvc.perform(get("/teste/explode"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.title").value("Erro interno"))
				.andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@RestController
	static class EndpointsDeTeste {

		@GetMapping("/teste/nao-encontrado")
		void naoEncontrado() {
			throw new RecursoNaoEncontradoException("Produto 42 não encontrado");
		}

		@GetMapping("/teste/duplicado")
		void duplicado() {
			throw new RecursoDuplicadoException("Código de barras já cadastrado");
		}

		@GetMapping("/teste/explode")
		void explode() {
			throw new IllegalStateException("boom");
		}

		@PostMapping("/teste/valida")
		void valida(@Valid @RequestBody Corpo corpo) {
		}

		record Corpo(@NotBlank(message = "não pode estar em branco") String nome) {
		}
	}
}
