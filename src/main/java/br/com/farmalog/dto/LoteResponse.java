package br.com.farmalog.dto;

import br.com.farmalog.entity.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(
		Long id,
		Long produtoId,
		String codigo,
		LocalDate dataValidade,
		Integer quantidadeAtual,
		BigDecimal precoCusto,
		LocalDate dataEntrada,
		boolean vencido
) {

	public static LoteResponse fromEntity(Lote lote) {
		return new LoteResponse(
				lote.getId(),
				lote.getProduto().getId(),
				lote.getCodigo(),
				lote.getDataValidade(),
				lote.getQuantidadeAtual(),
				lote.getPrecoCusto(),
				lote.getDataEntrada(),
				lote.isVencido()
		);
	}
}
