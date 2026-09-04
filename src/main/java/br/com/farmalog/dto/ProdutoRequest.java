package br.com.farmalog.dto;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Produto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(

		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
		String nome,

		@NotBlank(message = "O princípio ativo é obrigatório")
		@Size(max = 120, message = "O princípio ativo deve ter no máximo 120 caracteres")
		String principioAtivo,

		@NotBlank(message = "O fabricante é obrigatório")
		@Size(max = 120, message = "O fabricante deve ter no máximo 120 caracteres")
		String fabricante,

		@NotBlank(message = "O código de barras é obrigatório")
		@Size(max = 14, message = "O código de barras deve ter no máximo 14 caracteres")
		String codigoBarras,

		@NotNull(message = "O preço de venda é obrigatório")
		@Positive(message = "O preço de venda deve ser maior que zero")
		@Digits(integer = 10, fraction = 2, message = "O preço de venda deve ter no máximo 10 inteiros e 2 casas decimais")
		BigDecimal precoVenda,

		@NotNull(message = "A exigência de receita é obrigatória")
		ExigenciaReceita exigencia,

		@NotNull(message = "O estoque mínimo é obrigatório")
		@PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
		Integer estoqueMinimo
) {
	public Produto toEntity() {
		return Produto.builder()
				.nome(nome)
				.principioAtivo(principioAtivo)
				.fabricante(fabricante)
				.codigoBarras(codigoBarras)
				.precoVenda(precoVenda)
				.exigencia(exigencia)
				.estoqueMinimo(estoqueMinimo)
				.build();
	}
}
