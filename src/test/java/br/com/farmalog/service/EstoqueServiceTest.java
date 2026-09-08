package br.com.farmalog.service;

import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.TipoMovimentacao;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.MovimentacaoEstoqueRepository;
import br.com.farmalog.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

	@Mock
	MovimentacaoEstoqueRepository movimentacaoRepository;

	@Mock
	LoteRepository loteRepository;

	@Mock
	ProdutoRepository produtoRepository;

	@InjectMocks
	EstoqueService service;

	private final Usuario usuario = Usuario.builder().id(1L).email("farmaceutico@farmalog.dev").build();

	private Lote lote(int saldo) {
		return Lote.builder()
				.id(1L)
				.codigo("L1")
				.quantidadeAtual(saldo)
				.dataValidade(LocalDate.now().plusMonths(1))
				.build();
	}

	@Test
	void entrada_aumentaOSaldoERegistraAMovimentacao() {
		when(movimentacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		Lote lote = lote(10);

		var movimentacao = service.registrar(lote, TipoMovimentacao.ENTRADA, 5, usuario, null);

		assertThat(lote.getQuantidadeAtual()).isEqualTo(15);
		assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
		assertThat(movimentacao.getQuantidade()).isEqualTo(5);
		verify(movimentacaoRepository).save(any());
	}

	@Test
	void saida_reduzOSaldo() {
		when(movimentacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		Lote lote = lote(10);

		service.registrar(lote, TipoMovimentacao.SAIDA_VENDA, 4, usuario, null);

		assertThat(lote.getQuantidadeAtual()).isEqualTo(6);
	}

	@Test
	void estorno_aumentaOSaldo() {
		when(movimentacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		Lote lote = lote(10);

		service.registrar(lote, TipoMovimentacao.ESTORNO, 3, usuario, null);

		assertThat(lote.getQuantidadeAtual()).isEqualTo(13);
	}

	@Test
	void saldoInsuficiente_retorna422_eNaoRegistra() {
		Lote lote = lote(2);

		assertThatThrownBy(() -> service.registrar(lote, TipoMovimentacao.SAIDA_VENDA, 5, usuario, null))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		assertThat(lote.getQuantidadeAtual()).isEqualTo(2);
		verify(movimentacaoRepository, never()).save(any());
	}

	@Test
	void quantidadeNaoPositiva_retorna422() {
		assertThatThrownBy(() -> service.registrar(lote(10), TipoMovimentacao.ENTRADA, 0, usuario, null))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@Test
	void descarteSemObservacao_retorna422() {
		assertThatThrownBy(() -> service.registrar(lote(10), TipoMovimentacao.DESCARTE, 1, usuario, "  "))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		verify(movimentacaoRepository, never()).save(any());
	}

	@Test
	void descarteComObservacao_reduzOSaldo() {
		when(movimentacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		Lote lote = lote(10);

		service.registrar(lote, TipoMovimentacao.DESCARTE, 4, usuario, "lote vencido, incinerado");

		assertThat(lote.getQuantidadeAtual()).isEqualTo(6);
	}
}
