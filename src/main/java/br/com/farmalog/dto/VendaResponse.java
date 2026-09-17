package br.com.farmalog.dto;

import br.com.farmalog.entity.ItemVenda;
import br.com.farmalog.entity.StatusVenda;
import br.com.farmalog.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VendaResponse(
		Long id,
		String operadorEmail,
		String farmaceuticoEmail,
		String clienteCpf,
		Instant dataHora,
		BigDecimal valorTotal,
		StatusVenda status,
		List<ItemVendaResponse> itens
) {
	public static VendaResponse fromEntity(Venda venda, List<ItemVenda> itens) {
		return new VendaResponse(
				venda.getId(),
				venda.getOperador().getEmail(),
				venda.getFarmaceutico() == null ? null : venda.getFarmaceutico().getEmail(),
				venda.getClienteCpf(),
				venda.getDataHora(),
				venda.getValorTotal(),
				venda.getStatus(),
				itens.stream().map(ItemVendaResponse::fromEntity).toList()
		);
	}
}
