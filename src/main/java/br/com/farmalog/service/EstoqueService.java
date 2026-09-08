package br.com.farmalog.service;

import br.com.farmalog.dto.MovimentacaoFiltro;
import br.com.farmalog.dto.MovimentacaoResponse;
import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.MovimentacaoEstoque;
import br.com.farmalog.entity.TipoMovimentacao;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;

// Ponto único de alteração de saldo de lote (RN-04): toda mudança passa por aqui
// e deixa uma MovimentacaoEstoque.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstoqueService {

	private static final Set<TipoMovimentacao> AUMENTAM_SALDO =
			EnumSet.of(TipoMovimentacao.ENTRADA, TipoMovimentacao.ESTORNO);
	private static final Set<TipoMovimentacao> EXIGEM_OBSERVACAO =
			EnumSet.of(TipoMovimentacao.DESCARTE, TipoMovimentacao.AJUSTE);

	private final MovimentacaoEstoqueRepository movimentacaoRepository;

	@Transactional
	public MovimentacaoEstoque registrar(Lote lote, TipoMovimentacao tipo, int quantidade,
			Usuario usuario, String observacao) {

		if (quantidade <= 0) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A quantidade deve ser positiva");
		}
		if (EXIGEM_OBSERVACAO.contains(tipo) && (observacao == null || observacao.isBlank())) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"A observação é obrigatória em movimentação do tipo " + tipo);
		}

		int delta = AUMENTAM_SALDO.contains(tipo) ? quantidade : -quantidade;
		int novoSaldo = lote.getQuantidadeAtual() + delta;
		if (novoSaldo < 0) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Saldo insuficiente no lote " + lote.getCodigo() + " (atual: " + lote.getQuantidadeAtual() + ")");
		}

		lote.setQuantidadeAtual(novoSaldo);

		MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
				.lote(lote)
				.tipo(tipo)
				.quantidade(quantidade)
				.usuario(usuario)
				.observacao(observacao)
				.build();
		return movimentacaoRepository.save(movimentacao);
	}

	public Page<MovimentacaoResponse> listarMovimentacoes(MovimentacaoFiltro filtro, Pageable pageable) {
		Instant inicio = aInstante(filtro.inicio());
		Instant fim = filtro.fim() == null ? null : aInstante(filtro.fim().plusDays(1));
		return movimentacaoRepository
				.buscar(filtro.tipo(), filtro.loteId(), inicio, fim, pageable)
				.map(MovimentacaoResponse::fromEntity);
	}

	private static Instant aInstante(LocalDate data) {
		return data == null ? null : data.atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}
