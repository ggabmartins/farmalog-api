package br.com.farmalog.produto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "produto")
@Getter
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

	protected Produto() {
	}

	public Produto(String nome, String principioAtivo, String fabricante, String codigoBarras,
			BigDecimal precoVenda, ExigenciaReceita exigencia, Integer estoqueMinimo) {
		this.nome = nome;
		this.principioAtivo = principioAtivo;
		this.fabricante = fabricante;
		this.codigoBarras = codigoBarras;
		this.precoVenda = precoVenda;
		this.exigencia = exigencia;
		this.estoqueMinimo = estoqueMinimo;
		this.ativo = true;
	}

	public void atualizar(String nome, String principioAtivo, String fabricante, String codigoBarras,
			BigDecimal precoVenda, ExigenciaReceita exigencia, Integer estoqueMinimo) {
		this.nome = nome;
		this.principioAtivo = principioAtivo;
		this.fabricante = fabricante;
		this.codigoBarras = codigoBarras;
		this.precoVenda = precoVenda;
		this.exigencia = exigencia;
		this.estoqueMinimo = estoqueMinimo;
	}

	public void desativar() {
		this.ativo = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Produto outro)) {
			return false;
		}
		return id != null && id.equals(outro.id);
	}

	@Override
	public int hashCode() {
		return Produto.class.hashCode();
	}
}
