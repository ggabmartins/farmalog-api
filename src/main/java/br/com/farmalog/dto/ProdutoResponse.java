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
		int quantidadeEmEstoque,
		boolean ativo
) {

	public static ProdutoResponse fromEntity(Produto produto, int quantidadeEmEstoque) {
		return new ProdutoResponse(
				produto.getId(),
				produto.getNome(),
				produto.getPrincipioAtivo(),
				produto.getFabricante(),
				produto.getCodigoBarras(),
				produto.getPrecoVenda(),
				produto.getExigencia(),
				produto.getEstoqueMinimo(),
				quantidadeEmEstoque,
				produto.isAtivo()
		);
	}
}
