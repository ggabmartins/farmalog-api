package br.com.farmalog.service;

import br.com.farmalog.dto.DescarteRequest;
import br.com.farmalog.dto.LoteRequest;
import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.entity.TipoMovimentacao;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import br.com.farmalog.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

	@Mock
	LoteRepository loteRepository;

	@Mock
	ProdutoRepository produtoRepository;

	@Mock
	UsuarioRepository usuarioRepository;

	@Mock
	EstoqueService estoqueService;

	@InjectMocks
	LoteService service;

	private final Produto produto = Produto.builder()
			.id(1L).nome("Dipirona").exigencia(ExigenciaReceita.ISENTO).build();
	private final Usuario usuario = Usuario.builder()
			.id(1L).email("farmaceutico@farmalog.dev").build();

	private LoteRequest entrada(int quantidade) {
		return new LoteRequest("L1", LocalDate.now().plusMonths(6), quantidade,
				new BigDecimal("4.00"), LocalDate.now());
	}

	private Lote lote(int saldo, LocalDate validade) {
		return Lote.builder().id(5L).codigo("L5").produto(produto)
				.quantidadeAtual(saldo).dataValidade(validade).build();
	}

	@Test
	void registrarEntrada_produtoInexistente_retorna404() {
		when(produtoRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registrarEntrada(9L, entrada(100), usuario.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void registrarEntrada_codigoDuplicadoNoProduto_retorna409() {
		when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
		when(loteRepository.existsByProdutoIdAndCodigo(1L, "L1")).thenReturn(true);

		assertThatThrownBy(() -> service.registrarEntrada(1L, entrada(100), usuario.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);

		verify(loteRepository, never()).save(any());
	}

	@Test
	void registrarEntrada_salvaLoteComSaldoZero_eDelegaEntradaAoEstoqueService() {
		when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
		when(loteRepository.existsByProdutoIdAndCodigo(1L, "L1")).thenReturn(false);
		when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
		when(loteRepository.save(any(Lote.class))).thenAnswer(i -> i.getArgument(0));

		service.registrarEntrada(1L, entrada(100), usuario.getEmail());

		ArgumentCaptor<Lote> salvo = ArgumentCaptor.forClass(Lote.class);
		verify(loteRepository).save(salvo.capture());
		assertThat(salvo.getValue().getQuantidadeAtual()).isZero();
		verify(estoqueService).registrar(any(Lote.class), eq(TipoMovimentacao.ENTRADA), eq(100), eq(usuario), isNull());
	}

	@Test
	void descartar_loteNaoVencido_retorna422() {
		when(loteRepository.findById(5L)).thenReturn(Optional.of(lote(50, LocalDate.now().plusDays(5))));

		assertThatThrownBy(() -> service.descartar(5L, new DescarteRequest(10, "teste"), usuario.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		verify(estoqueService, never()).registrar(any(), any(), anyInt(), any(), any());
	}

	@Test
	void descartar_loteVencido_delegaDescarteAoEstoqueService() {
		Lote lote = lote(50, LocalDate.now().minusDays(2));
		when(loteRepository.findById(5L)).thenReturn(Optional.of(lote));
		when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

		service.descartar(5L, new DescarteRequest(50, "lote vencido, incinerado"), usuario.getEmail());

		verify(estoqueService).registrar(lote, TipoMovimentacao.DESCARTE, 50, usuario, "lote vencido, incinerado");
	}
}
