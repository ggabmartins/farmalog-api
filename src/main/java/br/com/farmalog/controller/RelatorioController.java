package br.com.farmalog.controller;

import br.com.farmalog.dto.ProdutoMaisVendidoResponse;
import br.com.farmalog.dto.RelatorioVendasResponse;
import br.com.farmalog.dto.VendaFiltro;
import br.com.farmalog.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GERENTE')")
@Tag(name = "Relatórios", description = "Relatórios gerenciais, somente para gerente")
public class RelatorioController {

	private final RelatorioService service;

	@GetMapping("/vendas")
	@Operation(summary = "Resumo de vendas do período: faturamento, ticket médio e valor perdido com cancelamentos")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resumo do período"),
			@ApiResponse(responseCode = "403", description = "Perfil sem permissão")
	})
	public RelatorioVendasResponse vendas(VendaFiltro filtro) {
		return service.vendas(filtro);
	}

	@GetMapping("/produtos-mais-vendidos")
	@Operation(summary = "Ranking de produtos por quantidade vendida, ignorando vendas canceladas (limite padrão 10, máximo 20)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Ranking, do mais vendido para o menos"),
			@ApiResponse(responseCode = "403", description = "Perfil sem permissão")
	})
	public List<ProdutoMaisVendidoResponse> produtosMaisVendidos(@RequestParam(required = false) Integer limite) {
		return service.maisVendidos(limite);
	}
}