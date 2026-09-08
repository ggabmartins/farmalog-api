package br.com.farmalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lote", uniqueConstraints =
		@UniqueConstraint(name = "uk_lote_produto_codigo", columnNames = {"produto_id", "codigo"}))
public class Lote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Column(nullable = false, length = 60)
	private String codigo;

	@Column(name = "data_validade", nullable = false)
	private LocalDate dataValidade;

	@Column(name = "quantidade_atual", nullable = false)
	private Integer quantidadeAtual;

	@Column(name = "preco_custo", nullable = false, precision = 12, scale = 2)
	private BigDecimal precoCusto;

	@Column(name = "data_entrada", nullable = false)
	private LocalDate dataEntrada;

	@Version
	@Column(nullable = false)
	private Long version;

	public boolean isVencido() {
		return dataValidade.isBefore(LocalDate.now());
	}
}
