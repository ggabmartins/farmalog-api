package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoFiltro;
import br.com.farmalog.dto.ProdutoRequest;
import br.com.farmalog.dto.ProdutoResponse;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProdutoService {

	private final ProdutoRepository repository;

	@Transactional
	public ProdutoResponse criar(ProdutoRequest req) {
		if (repository.existsByCodigoBarras(req.codigoBarras())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Já existe produto com o código de barras " + req.codigoBarras());
		}
		Produto produto = req.toEntity();
		produto.setAtivo(true);
		return ProdutoResponse.fromEntity(repository.save(produto));
	}

	public Page<ProdutoResponse> listar(ProdutoFiltro filtro, Pageable pageable) {
		boolean ativo = filtro.ativo() == null || filtro.ativo();
		return repository.buscar(filtro.nome(), filtro.principioAtivo(), filtro.exigencia(), ativo, pageable)
				.map(ProdutoResponse::fromEntity);
	}

	public ProdutoResponse buscarPorId(Long id) {
		return ProdutoResponse.fromEntity(buscarEntidade(id));
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
		return ProdutoResponse.fromEntity(produto);
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
}
