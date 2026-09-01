package br.com.farmalog.produto;

import br.com.farmalog.produto.dto.ProdutoRequest;
import br.com.farmalog.produto.entity.ExigenciaReceita;
import br.com.farmalog.produto.entity.Produto;
import br.com.farmalog.shared.exception.RecursoDuplicadoException;
import br.com.farmalog.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

	@InjectMocks
	ProdutoService service;

	private ProdutoRequest requestValido() {
		return new ProdutoRequest("Dipirona", "Dipirona sódica", "EMS", "7891234567890",
				new BigDecimal("12.90"), ExigenciaReceita.ISENTO, 10);
	}

	@Test
	void criar_comCodigoDeBarrasJaExistente_lancaRecursoDuplicado() {
		when(repository.existsByCodigoBarras("7891234567890")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(requestValido()))
				.isInstanceOf(RecursoDuplicadoException.class);

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
	void buscarPorId_inexistente_lancaRecursoNaoEncontrado() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining("99");
	}

	@Test
	void desativar_produtoExistente_marcaComoInativo() {
		Produto produto = new Produto("Dipirona", "Dipirona sódica", "EMS", "7891234567890",
				new BigDecimal("12.90"), ExigenciaReceita.ISENTO, 10);
		when(repository.findById(1L)).thenReturn(Optional.of(produto));

		service.desativar(1L);

		assertThat(produto.isAtivo()).isFalse();
	}
}
