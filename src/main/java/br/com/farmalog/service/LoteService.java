package br.com.farmalog.service;

import br.com.farmalog.dto.DescarteRequest;
import br.com.farmalog.dto.LoteRequest;
import br.com.farmalog.dto.LoteResponse;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import br.com.farmalog.entity.TipoMovimentacao;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.LoteRepository;
import br.com.farmalog.repository.ProdutoRepository;
import br.com.farmalog.repository.UsuarioRepository;
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
public class LoteService {

	private final LoteRepository loteRepository;
	private final ProdutoRepository produtoRepository;
	private final UsuarioRepository usuarioRepository;
	private final EstoqueService estoqueService;

	@Transactional
	public LoteResponse registrarEntrada(Long produtoId, LoteRequest req, String emailUsuario) {
		Produto produto = produtoRepository.findById(produtoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Produto " + produtoId + " não encontrado"));
		if (loteRepository.existsByProdutoIdAndCodigo(produtoId, req.codigo())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"O produto já tem um lote com o código " + req.codigo());
		}
		Usuario usuario = usuarioAtual(emailUsuario);

		Lote lote = req.toEntity(produto);
		lote.setQuantidadeAtual(0);
		lote = loteRepository.save(lote);

		estoqueService.registrar(lote, TipoMovimentacao.ENTRADA, req.quantidade(), usuario, null);
		return LoteResponse.fromEntity(lote);
	}

	public Page<LoteResponse> listarPorProduto(Long produtoId, Pageable pageable) {
		if (!produtoRepository.existsById(produtoId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + produtoId + " não encontrado");
		}
		return loteRepository.findByProdutoId(produtoId, pageable).map(LoteResponse::fromEntity);
	}

	public LoteResponse buscarPorId(Long produtoId, Long loteId) {
		Lote lote = loteRepository.findById(loteId)
				.filter(l -> l.getProduto().getId().equals(produtoId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote " + loteId + " não encontrado"));
		return LoteResponse.fromEntity(lote);
	}

	@Transactional
	public LoteResponse descartar(Long loteId, DescarteRequest req, String emailUsuario) {
		Lote lote = loteRepository.findById(loteId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote " + loteId + " não encontrado"));
		if (!lote.isVencido()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Só é permitido descarte de lote vencido");
		}
		Usuario usuario = usuarioAtual(emailUsuario);
		estoqueService.registrar(lote, TipoMovimentacao.DESCARTE, req.quantidade(), usuario, req.observacao());
		return LoteResponse.fromEntity(lote);
	}

	private Usuario usuarioAtual(String email) {
		return usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
	}
}
