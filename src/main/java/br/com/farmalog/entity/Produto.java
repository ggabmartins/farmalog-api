package br.com.farmalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "produto")
public class Produto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(name = "principio_ativo", nullable = false, length = 120)
	private String principioAtivo;

	@Column(nullable = false, length = 120)
	private String fabricante;

	@Column(name = "codigo_barras", nullable = false, length = 14)
	private String codigoBarras;

	@Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
	private BigDecimal precoVenda;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ExigenciaReceita exigencia;

	@Column(name = "estoque_minimo", nullable = false)
	private Integer estoqueMinimo;

	@Column(nullable = false)
	private boolean ativo;
}
