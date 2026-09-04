package br.com.farmalog.service;

import br.com.farmalog.dto.UsuarioRequest;
import br.com.farmalog.entity.Perfil;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	UsuarioRepository repository;

	@Mock
	PasswordEncoder passwordEncoder;

	@InjectMocks
	UsuarioService service;

	private UsuarioRequest requestValido() {
		return new UsuarioRequest("Ana", "ana@farmalog.dev", "farmalog123", Perfil.FARMACEUTICO);
	}

	@Test
	void criar_comEmailJaExistente_retorna409() {
		when(repository.existsByEmail("ana@farmalog.dev")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(requestValido()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);

		verify(repository, never()).save(any());
	}

	@Test
	void criar_hasheiaASenha() {
		when(repository.existsByEmail(any())).thenReturn(false);
		when(passwordEncoder.encode("farmalog123")).thenReturn("hash-bcrypt");
		when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		var response = service.criar(requestValido());

		assertThat(response.email()).isEqualTo("ana@farmalog.dev");
		verify(passwordEncoder).encode("farmalog123");
	}

	@Test
	void buscarPorId_inexistente_retorna404() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
	}
}
