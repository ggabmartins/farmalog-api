package br.com.farmalog.repository;

import br.com.farmalog.entity.ExigenciaReceita;
import br.com.farmalog.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

	boolean existsByCodigoBarras(String codigoBarras);

	boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

	@Query("""
			SELECT p FROM Produto p WHERE
			(:nome IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%'))) AND
			(:principioAtivo IS NULL OR LOWER(p.principioAtivo) LIKE LOWER(CONCAT('%', CAST(:principioAtivo AS string), '%'))) AND
			(:exigencia IS NULL OR p.exigencia = :exigencia) AND
			p.ativo = :ativo
			""")
	Page<Produto> buscar(@Param("nome") String nome,
			@Param("principioAtivo") String principioAtivo,
			@Param("exigencia") ExigenciaReceita exigencia,
			@Param("ativo") boolean ativo,
			Pageable pageable);

	@Query("""
			SELECT p FROM Produto p
			WHERE p.ativo = true AND p.estoqueMinimo > COALESCE(
				(SELECT SUM(l.quantidadeAtual) FROM Lote l
				 WHERE l.produto = p AND l.quantidadeAtual > 0 AND l.dataValidade >= :hoje), 0)
			ORDER BY p.nome
			""")
	List<Produto> abaixoDoMinimo(@Param("hoje") LocalDate hoje);
}
