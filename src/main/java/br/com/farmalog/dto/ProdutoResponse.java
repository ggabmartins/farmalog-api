package br.com.farmalog.dto;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Produto;

import java.math.BigDecimal;

public record ProdutoResponse(
		Long id,
		String nome,
		String principioAtivo,
		String fabricante,
		String codigoBarras,
		BigDecimal precoVenda,
		ExigenciaReceita exigencia,
		Integer estoqueMinimo,
		boolean ativo
) {

	public static ProdutoResponse from(Produto produto) {
		return new ProdutoResponse(
				produto.getId(),
				produto.getNome(),
				produto.getPrincipioAtivo(),
				produto.getFabricante(),
				produto.getCodigoBarras(),
				produto.getPrecoVenda(),
				produto.getExigencia(),
				produto.getEstoqueMinimo(),
				produto.isAtivo()
		);
	}
}
