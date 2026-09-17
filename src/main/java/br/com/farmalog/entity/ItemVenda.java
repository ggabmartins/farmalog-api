package br.com.farmalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "item_venda")
public class ItemVenda {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "venda_id", nullable = false)
	private Venda venda;

	@ManyToOne(optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Column(nullable = false)
	private Integer quantidade;

	@Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precoUnitario;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;
}
