package br.com.farmalog.dto;

import br.com.farmalog.entity.Perfil;
import br.com.farmalog.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
		String nome,

		@NotBlank(message = "O email é obrigatório")
		@Email(message = "Email inválido")
		@Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
		String email,

		@NotBlank(message = "A senha é obrigatória")
		@Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
		String senha,

		@NotNull(message = "O perfil é obrigatório")
		Perfil perfil
) {
	public Usuario toEntity(String senhaHash) {
		return Usuario.builder()
				.nome(nome)
				.email(email)
				.senhaHash(senhaHash)
				.perfil(perfil)
				.build();
	}
}
