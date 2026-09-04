package br.com.farmalog.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
	}

	public record LoginResponse(String token) {
	}

	public record UsuarioAutenticado(String email, String perfil) {
	}

	@PostMapping("/login")
	@Operation(summary = "Autentica por email e senha e devolve um JWT")
	public LoginResponse login(@RequestBody @Valid LoginRequest request) {
		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
		return new LoginResponse(tokenService.gerarToken(auth.getName()));
	}

	@GetMapping("/me")
	@Operation(summary = "Retorna o email e o perfil do usuário autenticado")
	public UsuarioAutenticado me(Authentication authentication) {
		String perfil = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(a -> a.startsWith("ROLE_"))
				.map(a -> a.substring("ROLE_".length()))
				.findFirst()
				.orElse(null);
		return new UsuarioAutenticado(authentication.getName(), perfil);
	}
}
