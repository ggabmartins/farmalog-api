package br.com.farmalog.service;

import br.com.farmalog.dto.ItemVendaRequest;
import br.com.farmalog.dto.ReceitaRequest;
import br.com.farmalog.dto.VendaRequest;
import br.com.farmalog.dto.VendaResponse;
import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.ItemVenda;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.MovimentacaoEstoque;
import br.com.farmalog.entity.Perfil;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.entity.StatusVenda;
import br.com.farmalog.entity.TipoMovimentacao;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.entity.Venda;
import br.com.farmalog.repository.ItemVendaRepository;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.MovimentacaoEstoqueRepository;
import br.com.farmalog.repository.ProdutoRepository;
import br.com.farmalog.repository.ReceitaRepository;
import br.com.farmalog.repository.UsuarioRepository;
import br.com.farmalog.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

	@Mock
	VendaRepository vendaRepository;

	@Mock
	ItemVendaRepository itemVendaRepository;

	@Mock
	ReceitaRepository receitaRepository;

	@Mock
	MovimentacaoEstoqueRepository movimentacaoRepository;

	@Mock
	ProdutoRepository produtoRepository;

	@Mock
	LoteRepository loteRepository;

	@Mock
	UsuarioRepository usuarioRepository;

	@Mock
	EstoqueService estoqueService;

	@InjectMocks
	VendaService service;

	private final Usuario atendente = Usuario.builder()
			.id(1L).email("atendente@farmalog.dev").perfil(Perfil.ATENDENTE).build();
	private final Usuario farmaceutico = Usuario.builder()
			.id(2L).email("farmaceutico@farmalog.dev").perfil(Perfil.FARMACEUTICO).build();

	private Produto produtoIsento() {
		return Produto.builder().id(10L).nome("Dipirona").exigencia(ExigenciaReceita.ISENTO)
				.precoVenda(new BigDecimal("10.00")).ativo(true).build();
	}

	private Produto produtoControlado() {
		return Produto.builder().id(20L).nome("Diazepam").exigencia(ExigenciaReceita.CONTROLADO)
				.precoVenda(new BigDecimal("30.00")).ativo(true).build();
	}

	private Lote lote(Long id, Produto produto, int saldo) {
		return Lote.builder().id(id).produto(produto).codigo("L" + id).quantidadeAtual(saldo)
				.dataValidade(LocalDate.now().plusMonths(1)).build();
	}

	@Test
	void registrar_consomeLotesPorOrdemDeFefo() {
		Produto produto = produtoIsento();
		Lote loteA = lote(1L, produto, 3);
		Lote loteB = lote(2L, produto, 5);

		when(usuarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
		when(itemVendaRepository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
		when(loteRepository.disponiveisFefo(eq(10L), any(LocalDate.class))).thenReturn(List.of(loteA, loteB));

		VendaRequest req = new VendaRequest(null, List.of(new ItemVendaRequest(10L, 4)), null);
		VendaResponse response = service.registrar(req, atendente.getEmail());

		verify(estoqueService).registrar(eq(loteA), eq(TipoMovimentacao.SAIDA_VENDA), eq(3),
				eq(atendente), isNull(), any(ItemVenda.class));
		verify(estoqueService).registrar(eq(loteB), eq(TipoMovimentacao.SAIDA_VENDA), eq(1),
				eq(atendente), isNull(), any(ItemVenda.class));
		assertThat(response.valorTotal()).isEqualByComparingTo(new BigDecimal("40.00"));
	}

	@Test
	void registrar_produtoRepetido_retorna422() {
		Produto produto = produtoIsento();
		Lote lote = lote(1L, produto, 10);

		when(usuarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
		when(itemVendaRepository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
		when(loteRepository.disponiveisFefo(eq(10L), any(LocalDate.class))).thenReturn(List.of(lote));

		VendaRequest req = new VendaRequest(null,
				List.of(new ItemVendaRequest(10L, 1), new ItemVendaRequest(10L, 2)), null);

		assertThatThrownBy(() -> service.registrar(req, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		verify(produtoRepository, times(1)).findById(10L);
	}

	@Test
	void registrar_produtoInexistente_retorna404() {
		when(usuarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

		VendaRequest req = new VendaRequest(null, List.of(new ItemVendaRequest(99L, 1)), null);

		assertThatThrownBy(() -> service.registrar(req, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void registrar_estoqueInsuficiente_retorna422() {
		Produto produto = produtoIsento();

		when(usuarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
		when(itemVendaRepository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
		when(loteRepository.disponiveisFefo(eq(10L), any(LocalDate.class))).thenReturn(List.of());

		VendaRequest req = new VendaRequest(null, List.of(new ItemVendaRequest(10L, 5)), null);

		assertThatThrownBy(() -> service.registrar(req, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@Test
	void registrar_naoIsentoSemReceita_retorna422() {
		Produto produto = produtoControlado();
		Lote lote = lote(1L, produto, 10);

		when(usuarioRepository.findByEmail(farmaceutico.getEmail())).thenReturn(Optional.of(farmaceutico));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(20L)).thenReturn(Optional.of(produto));
		when(itemVendaRepository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
		when(loteRepository.disponiveisFefo(eq(20L), any(LocalDate.class))).thenReturn(List.of(lote));

		VendaRequest req = new VendaRequest(null, List.of(new ItemVendaRequest(20L, 1)), null);

		assertThatThrownBy(() -> service.registrar(req, farmaceutico.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		verify(receitaRepository, never()).save(any());
	}

	@Test
	void registrar_atendenteVendeNaoIsento_retorna403() {
		Produto produto = produtoControlado();
		Lote lote = lote(1L, produto, 10);

		when(usuarioRepository.findByEmail(atendente.getEmail())).thenReturn(Optional.of(atendente));
		when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
		when(produtoRepository.findById(20L)).thenReturn(Optional.of(produto));
		when(itemVendaRepository.save(any(ItemVenda.class))).thenAnswer(i -> i.getArgument(0));
		when(loteRepository.disponiveisFefo(eq(20L), any(LocalDate.class))).thenReturn(List.of(lote));

		ReceitaRequest receita = new ReceitaRequest("123", "CRM1", LocalDate.now());
		VendaRequest req = new VendaRequest(null, List.of(new ItemVendaRequest(20L, 1)), receita);

		assertThatThrownBy(() -> service.registrar(req, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void cancelar_vendaInexistente_retorna404() {
		when(vendaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cancelar(99L, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void cancelar_vendaJaCancelada_retorna422() {
		Venda venda = Venda.builder().id(1L).status(StatusVenda.CANCELADA).build();
		when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));

		assertThatThrownBy(() -> service.cancelar(1L, atendente.getEmail()))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

		verify(movimentacaoRepository, never()).saidasDaVenda(any());
	}

	@Test
	void cancelar_estornaCadaMovimentacaoNoLoteDeOrigem() {
		Venda venda = Venda.builder().id(1L).status(StatusVenda.CONCLUIDA).build();
		Produto produto = produtoIsento();
		Lote loteA = lote(1L, produto, 0);
		Lote loteB = lote(2L, produto, 4);
		ItemVenda item = ItemVenda.builder().id(1L).produto(produto).quantidade(4).build();
		MovimentacaoEstoque saidaA = MovimentacaoEstoque.builder()
				.lote(loteA).tipo(TipoMovimentacao.SAIDA_VENDA).quantidade(3).itemVenda(item).usuario(atendente).build();
		MovimentacaoEstoque saidaB = MovimentacaoEstoque.builder()
				.lote(loteB).tipo(TipoMovimentacao.SAIDA_VENDA).quantidade(1).itemVenda(item).usuario(atendente).build();

		when(vendaRepository.findById(1L)).thenReturn(Optional.of(venda));
		when(usuarioRepository.findByEmail(farmaceutico.getEmail())).thenReturn(Optional.of(farmaceutico));
		when(movimentacaoRepository.saidasDaVenda(1L)).thenReturn(List.of(saidaA, saidaB));

		service.cancelar(1L, farmaceutico.getEmail());

		verify(estoqueService).registrar(loteA, TipoMovimentacao.ESTORNO, 3, farmaceutico, "Estorno da venda 1", item);
		verify(estoqueService).registrar(loteB, TipoMovimentacao.ESTORNO, 1, farmaceutico, "Estorno da venda 1", item);
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
	}
}
