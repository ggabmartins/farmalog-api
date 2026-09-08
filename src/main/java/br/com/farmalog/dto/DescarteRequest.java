package br.com.farmalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DescarteRequest(

		@NotNull(message = "A quantidade é obrigatória")
		@Positive(message = "A quantidade deve ser maior que zero")
		Integer quantidade,

		@NotBlank(message = "A observação é obrigatória")
		@Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
		String observacao
) {
}
