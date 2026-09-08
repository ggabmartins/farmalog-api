package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoFiltro;
import br.com.farmalog.dto.ProdutoRequest;
import br.com.farmalog.dto.ProdutoResponse;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProdutoService {

	private final ProdutoRepository repository;
	private final LoteRepository loteRepository;

	@Transactional
	public ProdutoResponse criar(ProdutoRequest req) {
		if (repository.existsByCodigoBarras(req.codigoBarras())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Já existe produto com o código de barras " + req.codigoBarras());
		}
		Produto produto = req.toEntity();
		produto.setAtivo(true);
		return ProdutoResponse.fromEntity(repository.save(produto), 0);
	}

	public Page<ProdutoResponse> listar(ProdutoFiltro filtro, Pageable pageable) {
		boolean ativo = filtro.ativo() == null || filtro.ativo();
		boolean abaixoDoMinimo = Boolean.TRUE.equals(filtro.abaixoDoMinimo());
		LocalDate hoje = LocalDate.now();

		Page<Produto> produtos = repository.buscar(filtro.nome(), filtro.principioAtivo(), filtro.exigencia(),
				ativo, abaixoDoMinimo, hoje, pageable);
		Map<Long, Integer> estoque = estoquePorProduto(produtos.getContent(), hoje);

		return produtos.map(p -> ProdutoResponse.fromEntity(p, estoque.getOrDefault(p.getId(), 0)));
	}

	public ProdutoResponse buscarPorId(Long id) {
		Produto produto = buscarEntidade(id);
		return ProdutoResponse.fromEntity(produto, loteRepository.disponivelPara(id, LocalDate.now()));
	}

	@Transactional
	public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
		Produto produto = buscarEntidade(id);
		if (repository.existsByCodigoBarrasAndIdNot(req.codigoBarras(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Já existe produto com o código de barras " + req.codigoBarras());
		}
		produto.setNome(req.nome());
		produto.setPrincipioAtivo(req.principioAtivo());
		produto.setFabricante(req.fabricante());
		produto.setCodigoBarras(req.codigoBarras());
		produto.setPrecoVenda(req.precoVenda());
		produto.setExigencia(req.exigencia());
		produto.setEstoqueMinimo(req.estoqueMinimo());
		return ProdutoResponse.fromEntity(produto, loteRepository.disponivelPara(id, LocalDate.now()));
	}

	@Transactional
	public void desativar(Long id) {
		buscarEntidade(id).setAtivo(false);
	}

	private Produto buscarEntidade(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Produto " + id + " não encontrado"));
	}

	private Map<Long, Integer> estoquePorProduto(List<Produto> produtos, LocalDate hoje) {
		if (produtos.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = produtos.stream().map(Produto::getId).toList();
		return loteRepository.disponivelPorProduto(ids, hoje).stream()
				.collect(Collectors.toMap(linha -> (Long) linha[0], linha -> ((Number) linha[1]).intValue()));
	}
}
