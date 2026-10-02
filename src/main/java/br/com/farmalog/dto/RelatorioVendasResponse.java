package br.com.farmalog.dto;

import java.math.BigDecimal;

public record RelatorioVendasResponse(
		long totalVendas,
		BigDecimal faturamento,
		BigDecimal ticketMedio,
		long totalCanceladas,
		BigDecimal valorCancelado
) {
}
