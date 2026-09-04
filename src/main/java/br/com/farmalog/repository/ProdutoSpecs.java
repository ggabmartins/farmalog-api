package br.com.farmalog.produto;

import br.com.farmalog.produto.dto.ProdutoFiltro;
import br.com.farmalog.produto.entity.ExigenciaReceita;
import br.com.farmalog.produto.entity.Produto;
import org.springframework.data.jpa.domain.Specification;

final class ProdutoSpecs {

	private ProdutoSpecs() {
	}

	static Specification<Produto> comFiltro(ProdutoFiltro filtro) {
		boolean ativo = filtro.ativo() == null || filtro.ativo();
		return Specification.allOf(
				nomeContem(filtro.nome()),
				principioAtivoContem(filtro.principioAtivo()),
				exigenciaIgualA(filtro.exigencia()),
				ativoIgualA(ativo)
		);
	}

	private static Specification<Produto> nomeContem(String nome) {
		return (root, query, cb) -> vazio(nome) ? null
				: cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
	}

	private static Specification<Produto> principioAtivoContem(String principioAtivo) {
		return (root, query, cb) -> vazio(principioAtivo) ? null
				: cb.like(cb.lower(root.get("principioAtivo")), "%" + principioAtivo.toLowerCase() + "%");
	}

	private static Specification<Produto> exigenciaIgualA(ExigenciaReceita exigencia) {
		return (root, query, cb) -> exigencia == null ? null : cb.equal(root.get("exigencia"), exigencia);
	}

	private static Specification<Produto> ativoIgualA(boolean ativo) {
		return (root, query, cb) -> cb.equal(root.get("ativo"), ativo);
	}

	private static boolean vazio(String valor) {
		return valor == null || valor.isBlank();
	}
}
