package br.com.farmalog.controller;

import br.com.farmalog.dto.VendaFiltro;
import br.com.farmalog.dto.VendaRequest;
import br.com.farmalog.dto.VendaResponse;
import br.com.farmalog.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/vendas")
@RequiredArgsConstructor
@Tag(name = "Vendas", description = "Registro e consulta de vendas")
public class VendaController {

	private final VendaService service;

	@PostMapping
	@Operation(summary = "Registra uma venda, com baixa de estoque por FEFO")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Venda registrada"),
			@ApiResponse(responseCode = "403", description = "Perfil sem permissão para vender produto com receita"),
			@ApiResponse(responseCode = "404", description = "Produto não encontrado"),
			@ApiResponse(responseCode = "422", description = "Item repetido, estoque insuficiente ou receita ausente")
	})
	public ResponseEntity<VendaResponse> registrar(@Valid @RequestBody VendaRequest req,
			Authentication authentication, UriComponentsBuilder uriBuilder) {
		VendaResponse venda = service.registrar(req, authentication.getName());
		URI location = uriBuilder.path("/api/v1/vendas/{id}").buildAndExpand(venda.id()).toUri();
		return ResponseEntity.created(location).body(venda);
	}

	@GetMapping
	@Operation(summary = "Lista vendas com paginação e filtro por período; atendente vê só as próprias")
	public Page<VendaResponse> listar(VendaFiltro filtro,
			@PageableDefault(sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication) {
		return service.listar(filtro, authentication.getName(), pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma venda por ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Venda encontrada"),
			@ApiResponse(responseCode = "404", description = "Venda não encontrada")
	})
	public VendaResponse buscar(@PathVariable Long id, Authentication authentication) {
		return service.buscarPorId(id, authentication.getName());
	}

	@PostMapping("/{id}/cancelamento")
	@PreAuthorize("hasAnyRole('FARMACEUTICO', 'GERENTE')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cancela uma venda, estornando cada movimentação de saída no lote de origem")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Venda cancelada"),
			@ApiResponse(responseCode = "404", description = "Venda não encontrada"),
			@ApiResponse(responseCode = "422", description = "Venda já está cancelada")
	})
	public void cancelar(@PathVariable Long id, Authentication authentication) {
		service.cancelar(id, authentication.getName());
	}
}
