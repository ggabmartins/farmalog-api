package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoFiltro;
import br.com.farmalog.dto.ProdutoRequest;
import br.com.farmalog.dto.ProdutoResponse;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.repository.ProdutoRepository;
import br.com.farmalog.repository.ProdutoSpecs;
import br.com.farmalog.validation.RecursoDuplicadoException;
import br.com.farmalog.validation.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

	private final ProdutoRepository repository;

	ProdutoService(ProdutoRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public ProdutoResponse criar(ProdutoRequest req) {
		if (repository.existsByCodigoBarras(req.codigoBarras())) {
			throw new RecursoDuplicadoException(
					"Já existe produto com o código de barras " + req.codigoBarras());
		}
		Produto produto = Produto.builder()
				.nome(req.nome())
				.principioAtivo(req.principioAtivo())
				.fabricante(req.fabricante())
				.codigoBarras(req.codigoBarras())
				.precoVenda(req.precoVenda())
				.exigencia(req.exigencia())
				.estoqueMinimo(req.estoqueMinimo())
				.ativo(true)
				.build();
		return ProdutoResponse.from(repository.save(produto));
	}

	public PagedModel<ProdutoResponse> listar(ProdutoFiltro filtro, Pageable pageable) {
		Page<ProdutoResponse> pagina = repository.findAll(ProdutoSpecs.comFiltro(filtro), pageable)
				.map(ProdutoResponse::from);
		return new PagedModel<>(pagina);
	}

	public ProdutoResponse buscarPorId(Long id) {
		return ProdutoResponse.from(buscarEntidade(id));
	}

	@Transactional
	public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
		Produto produto = buscarEntidade(id);
		if (repository.existsByCodigoBarrasAndIdNot(req.codigoBarras(), id)) {
			throw new RecursoDuplicadoException(
					"Já existe produto com o código de barras " + req.codigoBarras());
		}
		produto.setNome(req.nome());
		produto.setPrincipioAtivo(req.principioAtivo());
		produto.setFabricante(req.fabricante());
		produto.setCodigoBarras(req.codigoBarras());
		produto.setPrecoVenda(req.precoVenda());
		produto.setExigencia(req.exigencia());
		produto.setEstoqueMinimo(req.estoqueMinimo());
		return ProdutoResponse.from(produto);
	}

	@Transactional
	public void desativar(Long id) {
		buscarEntidade(id).desativar();
	}

	private Produto buscarEntidade(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado"));
	}
}
