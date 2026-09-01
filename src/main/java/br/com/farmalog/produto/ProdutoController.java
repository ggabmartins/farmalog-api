package br.com.farmalog.produto;

import br.com.farmalog.produto.dto.ProdutoRequest;
import br.com.farmalog.produto.dto.ProdutoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
class ProdutoController {

	private final ProdutoService service;

	ProdutoController(ProdutoService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest req,
			UriComponentsBuilder uriBuilder) {
		ProdutoResponse produto = service.criar(req);
		URI location = uriBuilder.path("/api/v1/produtos/{id}").buildAndExpand(produto.id()).toUri();
		return ResponseEntity.created(location).body(produto);
	}

	@GetMapping("/{id}")
	ProdutoResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PutMapping("/{id}")
	ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
		return service.atualizar(id, req);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void desativar(@PathVariable Long id) {
		service.desativar(id);
	}
}
