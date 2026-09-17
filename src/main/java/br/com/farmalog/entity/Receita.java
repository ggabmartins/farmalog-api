package br.com.farmalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "receita")
public class Receita {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "venda_id", nullable = false, unique = true)
	private Venda venda;

	@Column(nullable = false, length = 60)
	private String numero;

	@Column(name = "crm_medico", nullable = false, length = 30)
	private String crmMedico;

	@Column(name = "data_emissao", nullable = false)
	private LocalDate dataEmissao;

	@ManyToOne(optional = false)
	@JoinColumn(name = "farmaceutico_id", nullable = false)
	private Usuario farmaceutico;
}
