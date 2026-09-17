package br.com.farmalog.service;

import br.com.farmalog.dto.ItemVendaRequest;
import br.com.farmalog.dto.ReceitaRequest;
import br.com.farmalog.dto.VendaFiltro;
import br.com.farmalog.dto.VendaRequest;
import br.com.farmalog.dto.VendaResponse;
import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.ItemVenda;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.MovimentacaoEstoque;
import br.com.farmalog.entity.Perfil;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.entity.Receita;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendaService {

	private final VendaRepository vendaRepository;
	private final ItemVendaRepository itemVendaRepository;
	private final ReceitaRepository receitaRepository;
	private final MovimentacaoEstoqueRepository movimentacaoRepository;
	private final ProdutoRepository produtoRepository;
	private final LoteRepository loteRepository;
	private final UsuarioRepository usuarioRepository;
	private final EstoqueService estoqueService;

	@Transactional
	public VendaResponse registrar(VendaRequest req, String emailOperador) {
		Usuario operador = usuarioAtual(emailOperador);

		Venda venda = vendaRepository.save(Venda.builder()
				.operador(operador)
				.clienteCpf(req.clienteCpf())
				.status(StatusVenda.CONCLUIDA)
				.valorTotal(BigDecimal.ZERO)
				.build());

		Set<Long> produtosNoCarrinho = new HashSet<>();
		List<ItemVenda> itens = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		boolean exigeReceita = false;

		for (ItemVendaRequest linha : req.itens()) {
			if (!produtosNoCarrinho.add(linha.produtoId())) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
						"Produto " + linha.produtoId() + " repetido no carrinho");
			}

			Produto produto = produtoRepository.findById(linha.produtoId())
					.filter(Produto::isAtivo)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Produto " + linha.produtoId() + " não encontrado"));

			if (produto.getExigencia() != ExigenciaReceita.ISENTO) {
				exigeReceita = true;
			}

			ItemVenda item = itemVendaRepository.save(ItemVenda.builder()
					.venda(venda)
					.produto(produto)
					.quantidade(linha.quantidade())
					.precoUnitario(produto.getPrecoVenda())
					.subtotal(produto.getPrecoVenda().multiply(BigDecimal.valueOf(linha.quantidade())))
					.build());

			consumirEstoqueFefo(item, operador);

			itens.add(item);
			total = total.add(item.getSubtotal());
		}

		if (exigeReceita) {
			registrarReceita(req.receita(), venda, operador);
		}

		venda.setValorTotal(total);
		return VendaResponse.fromEntity(venda, itens);
	}

	@Transactional
	public void cancelar(Long vendaId, String emailUsuario) {
		Venda venda = vendaRepository.findById(vendaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Venda " + vendaId + " não encontrada"));
		if (venda.getStatus() == StatusVenda.CANCELADA) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Venda " + vendaId + " já está cancelada");
		}

		Usuario usuario = usuarioAtual(emailUsuario);

		for (MovimentacaoEstoque saida : movimentacaoRepository.saidasDaVenda(vendaId)) {
			estoqueService.registrar(saida.getLote(), TipoMovimentacao.ESTORNO, saida.getQuantidade(),
					usuario, "Estorno da venda " + vendaId, saida.getItemVenda());
		}

		venda.setStatus(StatusVenda.CANCELADA);
	}

	public Page<VendaResponse> listar(VendaFiltro filtro, String emailUsuario, Pageable pageable) {
		Usuario usuario = usuarioAtual(emailUsuario);
		Long operadorId = usuario.getPerfil() == Perfil.ATENDENTE ? usuario.getId() : null;

		Instant inicio = aInstante(filtro.inicio());
		Instant fim = filtro.fim() == null ? null : aInstante(filtro.fim().plusDays(1));

		Page<Venda> vendas = vendaRepository.buscar(operadorId, inicio, fim, pageable);

		List<Long> vendaIds = vendas.getContent().stream().map(Venda::getId).toList();
		Map<Long, List<ItemVenda>> itensPorVenda = itemVendaRepository.findByVendaIdIn(vendaIds).stream()
				.collect(Collectors.groupingBy(item -> item.getVenda().getId()));

		return vendas.map(venda -> VendaResponse.fromEntity(venda, itensPorVenda.getOrDefault(venda.getId(), List.of())));
	}

	public VendaResponse buscarPorId(Long id, String emailUsuario) {
		Venda venda = vendaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda " + id + " não encontrada"));

		Usuario usuario = usuarioAtual(emailUsuario);
		boolean vendaDeOutroAtendente = usuario.getPerfil() == Perfil.ATENDENTE
				&& !venda.getOperador().getId().equals(usuario.getId());
		if (vendaDeOutroAtendente) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda " + id + " não encontrada");
		}

		return VendaResponse.fromEntity(venda, itemVendaRepository.findByVendaId(id));
	}

	private void registrarReceita(ReceitaRequest receitaReq, Venda venda, Usuario operador) {
		if (operador.getPerfil() == Perfil.ATENDENTE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Atendente não pode vender produto que exige receita");
		}
		if (receitaReq == null) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Receita é obrigatória para produto que exige receita");
		}

		venda.setFarmaceutico(operador);
		receitaRepository.save(Receita.builder()
				.venda(venda)
				.numero(receitaReq.numero())
				.crmMedico(receitaReq.crmMedico())
				.dataEmissao(receitaReq.dataEmissao())
				.farmaceutico(operador)
				.build());
	}

	private void consumirEstoqueFefo(ItemVenda item, Usuario operador) {
		int restante = item.getQuantidade();

		for (Lote lote : loteRepository.disponiveisFefo(item.getProduto().getId(), LocalDate.now())) {
			if (restante == 0) break;
			int baixa = Math.min(restante, lote.getQuantidadeAtual());
			estoqueService.registrar(lote, TipoMovimentacao.SAIDA_VENDA, baixa, operador, null, item);
			restante -= baixa;
		}

		if (restante > 0) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Estoque insuficiente para " + item.getProduto().getNome());
		}
	}

	private Usuario usuarioAtual(String email) {
		return usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
	}

	private static Instant aInstante(LocalDate data) {
		return data == null ? null : data.atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}
