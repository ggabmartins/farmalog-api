package br.com.farmalog.dto;

public record ProdutoMaisVendidoResponse(
		Long produtoId,
		String nome,
		long quantidadeVendida
) {
}
