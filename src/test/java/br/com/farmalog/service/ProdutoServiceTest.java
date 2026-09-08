package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoRequest;
import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

	@Mock
	ProdutoRepository repository;

	@Mock
	LoteRepository loteRepository;

	@InjectMocks
	ProdutoService service;

	private ProdutoRequest requestValido() {
		return new ProdutoRequest("Dipirona", "Dipirona sódica", "EMS", "7891234567890",
				new BigDecimal("12.90"), ExigenciaReceita.ISENTO, 10);
	}

	@Test
	void criar_comCodigoDeBarrasJaExistente_retorna409() {
		when(repository.existsByCodigoBarras("7891234567890")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(requestValido()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);

		verify(repository, never()).save(any());
	}

	@Test
	void criar_comCodigoInedito_salvaERetornaResponse() {
		when(repository.existsByCodigoBarras(any())).thenReturn(false);
		when(repository.save(any(Produto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		var response = service.criar(requestValido());

		assertThat(response.nome()).isEqualTo("Dipirona");
		assertThat(response.ativo()).isTrue();
		verify(repository).save(any(Produto.class));
	}

	@Test
	void buscarPorId_inexistente_retorna404() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void desativar_produtoExistente_marcaComoInativo() {
		Produto produto = Produto.builder()
				.nome("Dipirona")
				.codigoBarras("7891234567890")
				.precoVenda(new BigDecimal("12.90"))
				.exigencia(ExigenciaReceita.ISENTO)
				.estoqueMinimo(10)
				.ativo(true)
				.build();
		when(repository.findById(1L)).thenReturn(Optional.of(produto));

		service.desativar(1L);

		assertThat(produto.isAtivo()).isFalse();
	}
}
