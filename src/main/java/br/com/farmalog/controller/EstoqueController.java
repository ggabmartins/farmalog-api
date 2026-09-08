package br.com.farmalog.controller;

import br.com.farmalog.dto.AlertasResponse;
import br.com.farmalog.dto.MovimentacaoFiltro;
import br.com.farmalog.dto.MovimentacaoResponse;
import br.com.farmalog.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Auditoria de movimentações e alertas")
public class EstoqueController {

	private final EstoqueService service;

	@GetMapping("/movimentacoes")
	@Operation(summary = "Lista as movimentações de estoque, com filtros por tipo, lote e período")
	public Page<MovimentacaoResponse> movimentacoes(MovimentacaoFiltro filtro,
			@PageableDefault(sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listarMovimentacoes(filtro, pageable);
	}

	@GetMapping("/alertas")
	@Operation(summary = "Produtos abaixo do estoque mínimo e lotes vencendo dentro de N dias")
	public AlertasResponse alertas(@RequestParam(defaultValue = "30") int dias) {
		return service.alertas(dias);
	}
}
