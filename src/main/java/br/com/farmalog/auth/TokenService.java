package br.com.farmalog.auth;

import br.com.farmalog.entity.Usuario;
import br.com.farmalog.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

	private static final long EXPIRACAO_MINUTOS = 30;

	private final JwtEncoder encoder;
	private final UsuarioRepository usuarioRepository;

	public String gerarToken(String email) {
		Usuario usuario = usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("farmalog-api")
				.issuedAt(agora)
				.expiresAt(agora.plus(EXPIRACAO_MINUTOS, ChronoUnit.MINUTES))
				.subject(usuario.getEmail())
				.claim("role", usuario.getPerfil().name())
				.build();

		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
