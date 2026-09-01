package br.com.farmalog.produto.dto;

import br.com.farmalog.produto.entity.ExigenciaReceita;

public record ProdutoFiltro(
		String nome,
		String principioAtivo,
		ExigenciaReceita exigencia,
		Boolean ativo
) {
}
