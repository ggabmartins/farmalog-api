package br.com.farmalog.dto;

import br.com.farmalog.entity.ExigenciaReceita;

public record ProdutoFiltro(
		String nome,
		String principioAtivo,
		ExigenciaReceita exigencia,
		Boolean ativo
) {
}
