package br.com.farmalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "venda")
public class Venda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "operador_id", nullable = false)
	private Usuario operador;

	@ManyToOne
	@JoinColumn(name = "farmaceutico_id")
	private Usuario farmaceutico;

	@Column(name = "cliente_cpf", length = 11)
	private String clienteCpf;

	@CreationTimestamp
	@Column(name = "data_hora", nullable = false, updatable = false)
	private Instant dataHora;

	@Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusVenda status;
}
