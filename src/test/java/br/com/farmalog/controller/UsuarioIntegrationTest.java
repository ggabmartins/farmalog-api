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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	private static RequestPostProcessor gerente() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_GERENTE"));
	}

	private static RequestPostProcessor farmaceutico() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"));
	}

	private static final String CORPO = """
			{
			  "nome": "Ana Souza",
			  "email": "ana.souza@farmalog.dev",
			  "senha": "farmalog123",
			  "perfil": "FARMACEUTICO"
			}
			""";

	@Test
	void gerente_criaEListaUsuario() throws Exception {
		mockMvc.perform(post("/api/v1/usuarios").with(gerente())
						.contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfil").value("FARMACEUTICO"))
				.andExpect(jsonPath("$.ativo").value(true));

		mockMvc.perform(get("/api/v1/usuarios").with(gerente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isNotEmpty());
	}

	@Test
	void naoGerente_naoAcessaUsuarios_retorna403() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios").with(farmaceutico()))
				.andExpect(status().isForbidden());
	}
}
