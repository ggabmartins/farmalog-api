package br.com.farmalog.dto;

import java.time.LocalDate;
import java.util.List;

public record AlertasResponse(
		List<ProdutoAbaixoDoMinimo> produtosAbaixoDoMinimo,
		List<LoteVencendo> lotesVencendo
) {

	public record ProdutoAbaixoDoMinimo(
			Long produtoId,
			String nome,
			int estoqueMinimo,
			int disponivel
	) {
	}

	public record LoteVencendo(
			Long loteId,
			Long produtoId,
			String produtoNome,
			String codigo,
			LocalDate dataValidade,
			int quantidadeAtual,
			long diasParaVencer
	) {
	}
}
