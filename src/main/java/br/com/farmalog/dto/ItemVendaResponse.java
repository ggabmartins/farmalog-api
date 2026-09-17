package br.com.farmalog.dto;

import br.com.farmalog.entity.ItemVenda;

import java.math.BigDecimal;

public record ItemVendaResponse(
		Long produtoId,
		String produtoNome,
		Integer quantidade,
		BigDecimal precoUnitario,
		BigDecimal subtotal
) {
	public static ItemVendaResponse fromEntity(ItemVenda item) {
		return new ItemVendaResponse(
				item.getProduto().getId(),
				item.getProduto().getNome(),
				item.getQuantidade(),
				item.getPrecoUnitario(),
				item.getSubtotal()
		);
	}
}
