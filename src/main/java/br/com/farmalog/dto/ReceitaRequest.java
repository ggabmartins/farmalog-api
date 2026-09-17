package br.com.farmalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReceitaRequest(

		@NotBlank(message = "O número da receita é obrigatório")
		@Size(max = 60, message = "O número da receita deve ter no máximo 60 caracteres")
		String numero,

		@NotBlank(message = "O CRM do médico é obrigatório")
		@Size(max = 30, message = "O CRM do médico deve ter no máximo 30 caracteres")
		String crmMedico,

		@NotNull(message = "A data de emissão é obrigatória")
		@PastOrPresent(message = "A data de emissão não pode estar no futuro")
		LocalDate dataEmissao
) {
}
