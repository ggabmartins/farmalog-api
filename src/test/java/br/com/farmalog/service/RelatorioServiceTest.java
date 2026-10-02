package br.com.farmalog.service;

import br.com.farmalog.dto.ProdutoMaisVendidoResponse;
import br.com.farmalog.dto.RelatorioVendasResponse;
import br.com.farmalog.dto.VendaFiltro;
import br.com.farmalog.entity.StatusVenda;
import br.com.farmalog.repository.ItemVendaRepository;
import br.com.farmalog.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

	@Mock
	VendaRepository vendaRepository;

	@Mock
	ItemVendaRepository itemVendaRepository;

	@InjectMocks
	RelatorioService service;

	private static Object[] linha(StatusVenda status, long quantidade, String total) {
		return new Object[]{status, quantidade, new BigDecimal(total)};
	}

	private static List<Object[]> resumo(Object[]... linhas) {
		return List.of(linhas);
	}

	private static final VendaFiltro SEM_FILTRO = new VendaFiltro(null, null);

	@Test
	void vendas_ticketMedioDivideSoPorConcluidas_eArredondaParaDuasCasas() {
		when(vendaRepository.resumirPorStatus(any(), any()))
				.thenReturn(resumo(linha(StatusVenda.CONCLUIDA, 6, "100.00")));

		RelatorioVendasResponse resposta = service.vendas(SEM_FILTRO);

		assertThat(resposta.totalVendas()).isEqualTo(6);
		assertThat(resposta.faturamento()).isEqualByComparingTo("100.00");
		assertThat(resposta.ticketMedio()).isEqualTo(new BigDecimal("16.67"));
	}

	@Test
	void vendas_canceladasFicamForaDoFaturamento_eViramValorCancelado() {
		when(vendaRepository.resumirPorStatus(any(), any())).thenReturn(resumo(
				linha(StatusVenda.CONCLUIDA, 2, "30.00"),
				linha(StatusVenda.CANCELADA, 1, "30.00")));

		RelatorioVendasResponse resposta = service.vendas(SEM_FILTRO);

		assertThat(resposta.totalVendas()).isEqualTo(2);
		assertThat(resposta.faturamento()).isEqualByComparingTo("30.00");
		assertThat(resposta.ticketMedio()).isEqualByComparingTo("15.00");
		assertThat(resposta.totalCanceladas()).isEqualTo(1);
		assertThat(resposta.valorCancelado()).isEqualByComparingTo("30.00");
	}

	@Test
	void vendas_semNenhumaVenda_devolveZerosSemDividirPorZero() {
		when(vendaRepository.resumirPorStatus(any(), any())).thenReturn(List.of());

		RelatorioVendasResponse resposta = service.vendas(SEM_FILTRO);

		assertThat(resposta.totalVendas()).isZero();
		assertThat(resposta.totalCanceladas()).isZero();
		assertThat(resposta.faturamento()).isEqualTo(new BigDecimal("0.00"));
		assertThat(resposta.ticketMedio()).isEqualTo(new BigDecimal("0.00"));
		assertThat(resposta.valorCancelado()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	void vendas_soTemCanceladas_faturamentoEhZeroEOTicketTambem() {
		when(vendaRepository.resumirPorStatus(any(), any()))
				.thenReturn(resumo(linha(StatusVenda.CANCELADA, 4, "80.00")));

		RelatorioVendasResponse resposta = service.vendas(SEM_FILTRO);

		assertThat(resposta.totalVendas()).isZero();
		assertThat(resposta.faturamento()).isEqualByComparingTo("0.00");
		assertThat(resposta.ticketMedio()).isEqualByComparingTo("0.00");
		assertThat(resposta.totalCanceladas()).isEqualTo(4);
		assertThat(resposta.valorCancelado()).isEqualByComparingTo("80.00");
	}

	@Test
	void vendas_diaFinalDoPeriodoEntraInteiro() {
		when(vendaRepository.resumirPorStatus(any(), any())).thenReturn(List.of());

		service.vendas(new VendaFiltro(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));

		verify(vendaRepository).resumirPorStatus(
				Instant.parse("2026-10-01T00:00:00Z"),
				Instant.parse("2026-10-03T00:00:00Z"));
	}

	@Test
	void maisVendidos_aplicaPadraoETetoNoLimite() {
		when(itemVendaRepository.maisVendidos(any())).thenReturn(List.of());

		service.maisVendidos(null);
		service.maisVendidos(500);
		service.maisVendidos(0);
		service.maisVendidos(-5);
		service.maisVendidos(7);

		verify(itemVendaRepository).maisVendidos(PageRequest.of(0, 10));
		verify(itemVendaRepository).maisVendidos(PageRequest.of(0, 20));
		verify(itemVendaRepository, times(2)).maisVendidos(PageRequest.of(0, 1));
		verify(itemVendaRepository).maisVendidos(PageRequest.of(0, 7));
	}

	@Test
	void maisVendidos_converteAsLinhasDaQueryEmResposta() {
		when(itemVendaRepository.maisVendidos(any())).thenReturn(resumo(
				new Object[]{1L, "Dipirona", 8L},
				new Object[]{2L, "Amoxicilina", 5L}));

		List<ProdutoMaisVendidoResponse> ranking = service.maisVendidos(null);

		assertThat(ranking).containsExactly(
				new ProdutoMaisVendidoResponse(1L, "Dipirona", 8),
				new ProdutoMaisVendidoResponse(2L, "Amoxicilina", 5));
	}
}
