package br.com.farmalog.produto;

import br.com.farmalog.produto.dto.ProdutoRequest;
import br.com.farmalog.produto.dto.ProdutoResponse;
import br.com.farmalog.produto.entity.Produto;
import br.com.farmalog.shared.exception.RecursoDuplicadoException;
import br.com.farmalog.shared.exception.RecursoNaoEncontradoException;
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
		Produto produto = new Produto(req.nome(), req.principioAtivo(), req.fabricante(),
				req.codigoBarras(), req.precoVenda(), req.exigencia(), req.estoqueMinimo());
		return ProdutoResponse.from(repository.save(produto));
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
		produto.atualizar(req.nome(), req.principioAtivo(), req.fabricante(),
				req.codigoBarras(), req.precoVenda(), req.exigencia(), req.estoqueMinimo());
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
