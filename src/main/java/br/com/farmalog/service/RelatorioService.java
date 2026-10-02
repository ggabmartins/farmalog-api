package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoMaisVendidoResponse;
import br.com.farmalog.dto.RelatorioVendasResponse;
import br.com.farmalog.dto.VendaFiltro;
import br.com.farmalog.entity.StatusVenda;
import br.com.farmalog.repository.ItemVendaRepository;
import br.com.farmalog.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioService {

	private static final BigDecimal ZERO_REAIS = BigDecimal.ZERO.setScale(2);

	private static final int LIMITE_PADRAO = 10;
	private static final int LIMITE_MAXIMO = 20;

	private final VendaRepository vendaRepository;
	private final ItemVendaRepository itemVendaRepository;

	public RelatorioVendasResponse vendas(VendaFiltro filtro) {
		Instant inicio = aInstante(filtro.inicio());
		Instant fim = filtro.fim() == null ? null : aInstante(filtro.fim().plusDays(1));

		long totalVendas = 0;
		BigDecimal faturamento = ZERO_REAIS;
		long totalCanceladas = 0;
		BigDecimal valorCancelado = ZERO_REAIS;

		for (Object[] linha : vendaRepository.resumirPorStatus(inicio, fim)) {
			StatusVenda status = (StatusVenda) linha[0];
			long quantidade = (Long) linha[1];
			BigDecimal total = (BigDecimal) linha[2];

			if (status == StatusVenda.CONCLUIDA) {
				totalVendas = quantidade;
				faturamento = total;
			} else if (status == StatusVenda.CANCELADA) {
				totalCanceladas = quantidade;
				valorCancelado = total;
			}
		}

		BigDecimal ticketMedio = totalVendas == 0
				? ZERO_REAIS
				: faturamento.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP);

		return new RelatorioVendasResponse(totalVendas, faturamento, ticketMedio, totalCanceladas, valorCancelado);
	}

	public List<ProdutoMaisVendidoResponse> maisVendidos(Integer limite) {
		int efetivo = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));

		return itemVendaRepository.maisVendidos(PageRequest.of(0, efetivo)).stream()
				.map(linha -> new ProdutoMaisVendidoResponse((Long) linha[0], (String) linha[1], (Long) linha[2]))
				.toList();
	}

	private static Instant aInstante(LocalDate data) {
		return data == null ? null : data.atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}
