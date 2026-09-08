package br.com.farmalog.dto;

import br.com.farmalog.entity.Lote;
import br.com.farmalog.entity.Produto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteRequest(

		@NotBlank(message = "O código do lote é obrigatório")
		@Size(max = 60, message = "O código do lote deve ter no máximo 60 caracteres")
		String codigo,

		@NotNull(message = "A data de validade é obrigatória")
		@FutureOrPresent(message = "A data de validade não pode estar no passado")
		LocalDate dataValidade,

		@NotNull(message = "A quantidade é obrigatória")
		@Positive(message = "A quantidade deve ser maior que zero")
		Integer quantidade,

		@NotNull(message = "O preço de custo é obrigatório")
		@Positive(message = "O preço de custo deve ser maior que zero")
		@Digits(integer = 10, fraction = 2, message = "O preço de custo deve ter no máximo 10 inteiros e 2 casas decimais")
		BigDecimal precoCusto,

		@NotNull(message = "A data de entrada é obrigatória")
		@PastOrPresent(message = "A data de entrada não pode estar no futuro")
		LocalDate dataEntrada
) {
	public Lote toEntity(Produto produto) {
		return Lote.builder()
				.produto(produto)
				.codigo(codigo)
				.dataValidade(dataValidade)
				.precoCusto(precoCusto)
				.dataEntrada(dataEntrada)
				.build();
	}
}
