package br.com.farmalog.dto;

import br.com.farmalog.entity.MovimentacaoEstoque;
import br.com.farmalog.entity.TipoMovimentacao;

import java.time.Instant;

public record MovimentacaoResponse(
		Long id,
		Long loteId,
		String loteCodigo,
		String produtoNome,
		TipoMovimentacao tipo,
		Integer quantidade,
		String usuarioEmail,
		Instant dataHora,
		String observacao
) {

	public static MovimentacaoResponse fromEntity(MovimentacaoEstoque m) {
		return new MovimentacaoResponse(
				m.getId(),
				m.getLote().getId(),
				m.getLote().getCodigo(),
				m.getLote().getProduto().getNome(),
				m.getTipo(),
				m.getQuantidade(),
				m.getUsuario().getEmail(),
				m.getDataHora(),
				m.getObservacao()
		);
	}
}
