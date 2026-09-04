package br.com.farmalog.service;

import br.com.farmalog.dto.UsuarioRequest;
import br.com.farmalog.dto.UsuarioResponse;
import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository repository;
	private final PasswordEncoder passwordEncoder;

	public Page<UsuarioResponse> listar(Pageable pageable) {
		return repository.findAll(pageable).map(UsuarioResponse::fromEntity);
	}

	public UsuarioResponse buscarPorId(Long id) {
		return UsuarioResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest req) {
		if (repository.existsByEmail(req.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe usuário com o email " + req.email());
		}
		Usuario usuario = req.toEntity(passwordEncoder.encode(req.senha()));
		usuario.setAtivo(true);
		return UsuarioResponse.fromEntity(repository.save(usuario));
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioRequest req) {
		Usuario usuario = buscarEntidade(id);
		if (repository.existsByEmailAndIdNot(req.email(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe usuário com o email " + req.email());
		}
		usuario.setNome(req.nome());
		usuario.setEmail(req.email());
		usuario.setPerfil(req.perfil());
		usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
		return UsuarioResponse.fromEntity(usuario);
	}

	@Transactional
	public void desativar(Long id) {
		buscarEntidade(id).setAtivo(false);
	}

	private Usuario buscarEntidade(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário " + id + " não encontrado"));
	}
}
