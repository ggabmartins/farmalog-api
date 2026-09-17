package br.com.farmalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record VendaRequest(

		@Pattern(regexp = "\\d{11}", message = "O CPF deve ter 11 dígitos")
		String clienteCpf,

		@NotEmpty(message = "A venda precisa de pelo menos um item")
		@Valid
		List<ItemVendaRequest> itens
) {
}
