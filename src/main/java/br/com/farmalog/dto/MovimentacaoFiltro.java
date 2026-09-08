package br.com.farmalog.dto;

import br.com.farmalog.entity.TipoMovimentacao;

import java.time.LocalDate;

public record MovimentacaoFiltro(
		TipoMovimentacao tipo,
		Long loteId,
		LocalDate inicio,
		LocalDate fim
) {
}
