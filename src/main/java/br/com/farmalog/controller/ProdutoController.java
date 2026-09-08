package br.com.farmalog.controller;

import br.com.farmalog.dto.ProdutoFiltro;
import br.com.farmalog.dto.ProdutoRequest;
import br.com.farmalog.dto.ProdutoResponse;
import br.com.farmalog.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Cadastro de produtos com paginação, busca por filtros e desativação lógica")
public class ProdutoController {

	private final ProdutoService service;

	@GetMapping
	@Operation(summary = "Lista produtos com paginação e filtros opcionais por nome, princípio ativo, exigência, ativo e abaixo do mínimo")
	public Page<ProdutoResponse> listar(ProdutoFiltro filtro,
			@PageableDefault(sort = "nome") Pageable pageable) {
		return service.listar(filtro, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca produto por ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Produto encontrado"),
			@ApiResponse(responseCode = "404", description = "Produto não encontrado")
	})
	public ProdutoResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('GERENTE')")
	@Operation(summary = "Cria um novo produto")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Produto criado"),
			@ApiResponse(responseCode = "400", description = "Erro de validação"),
			@ApiResponse(responseCode = "409", description = "Código de barras já cadastrado")
	})
	public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest req,
			UriComponentsBuilder uriBuilder) {
		ProdutoResponse produto = service.criar(req);
		URI location = uriBuilder.path("/api/v1/produtos/{id}").buildAndExpand(produto.id()).toUri();
		return ResponseEntity.created(location).body(produto);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('GERENTE')")
	@Operation(summary = "Atualiza um produto existente")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Produto atualizado"),
			@ApiResponse(responseCode = "400", description = "Erro de validação"),
			@ApiResponse(responseCode = "404", description = "Produto não encontrado"),
			@ApiResponse(responseCode = "409", description = "Código de barras já cadastrado")
	})
	public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
		return service.atualizar(id, req);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('GERENTE')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Desativa um produto (remoção lógica)")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Produto desativado"),
			@ApiResponse(responseCode = "404", description = "Produto não encontrado")
	})
	public void desativar(@PathVariable Long id) {
		service.desativar(id);
	}
}
