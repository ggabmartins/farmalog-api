package br.com.farmalog.controller;

import br.com.farmalog.dto.UsuarioRequest;
import br.com.farmalog.dto.UsuarioResponse;
import br.com.farmalog.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GERENTE')")
@Tag(name = "Usuários", description = "Gestão de usuários e perfis — restrito ao gerente")
public class UsuarioController {

	private final UsuarioService service;

	@GetMapping
	@Operation(summary = "Lista usuários com paginação")
	public Page<UsuarioResponse> listar(@PageableDefault(sort = "nome") Pageable pageable) {
		return service.listar(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca usuário por ID")
	public UsuarioResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	@Operation(summary = "Cria um novo usuário (senha hasheada com BCrypt)")
	public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest req,
			UriComponentsBuilder uriBuilder) {
		UsuarioResponse usuario = service.criar(req);
		URI location = uriBuilder.path("/api/v1/usuarios/{id}").buildAndExpand(usuario.id()).toUri();
		return ResponseEntity.created(location).body(usuario);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um usuário existente")
	public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
		return service.atualizar(id, req);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Desativa um usuário (remoção lógica)")
	public void desativar(@PathVariable Long id) {
		service.desativar(id);
	}
}
