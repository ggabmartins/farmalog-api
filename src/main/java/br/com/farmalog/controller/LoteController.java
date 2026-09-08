package br.com.farmalog.controller;

import br.com.farmalog.dto.DescarteRequest;
import br.com.farmalog.dto.LoteRequest;
import br.com.farmalog.dto.LoteResponse;
import br.com.farmalog.service.LoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Lotes", description = "Entrada de estoque por lote e controle de validade")
public class LoteController {

	private final LoteService service;

	@GetMapping("/produtos/{produtoId}/lotes")
	@Operation(summary = "Lista os lotes de um produto")
	public Page<LoteResponse> listar(@PathVariable Long produtoId,
			@PageableDefault(sort = "dataValidade") Pageable pageable) {
		return service.listarPorProduto(produtoId, pageable);
	}

	@GetMapping("/produtos/{produtoId}/lotes/{loteId}")
	@Operation(summary = "Busca um lote por ID")
	public LoteResponse buscar(@PathVariable Long produtoId, @PathVariable Long loteId) {
		return service.buscarPorId(produtoId, loteId);
	}

	@PostMapping("/produtos/{produtoId}/lotes")
	@PreAuthorize("hasAnyRole('FARMACEUTICO', 'GERENTE')")
	@Operation(summary = "Registra a entrada de um lote no estoque")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Lote registrado"),
			@ApiResponse(responseCode = "404", description = "Produto não encontrado"),
			@ApiResponse(responseCode = "409", description = "Já existe lote com esse código para o produto")
	})
	public ResponseEntity<LoteResponse> registrarEntrada(@PathVariable Long produtoId,
			@Valid @RequestBody LoteRequest req,
			Authentication authentication,
			UriComponentsBuilder uriBuilder) {
		LoteResponse lote = service.registrarEntrada(produtoId, req, authentication.getName());
		URI location = uriBuilder.path("/api/v1/produtos/{produtoId}/lotes/{loteId}")
				.buildAndExpand(produtoId, lote.id()).toUri();
		return ResponseEntity.created(location).body(lote);
	}

	@GetMapping("/lotes/vencendo")
	@Operation(summary = "Lista lotes com saldo que vencem dentro de N dias")
	public Page<LoteResponse> vencendo(@RequestParam(defaultValue = "30") int dias,
			@PageableDefault(sort = "dataValidade") Pageable pageable) {
		return service.listarVencendo(dias, pageable);
	}

	@PostMapping("/lotes/{loteId}/descarte")
	@PreAuthorize("hasAnyRole('FARMACEUTICO', 'GERENTE')")
	@Operation(summary = "Descarta um lote vencido (parcial ou total), com observação obrigatória")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Descarte registrado"),
			@ApiResponse(responseCode = "404", description = "Lote não encontrado"),
			@ApiResponse(responseCode = "422", description = "Lote não vencido ou saldo insuficiente")
	})
	public LoteResponse descartar(@PathVariable Long loteId, @Valid @RequestBody DescarteRequest req,
			Authentication authentication) {
		return service.descartar(loteId, req, authentication.getName());
	}
}
