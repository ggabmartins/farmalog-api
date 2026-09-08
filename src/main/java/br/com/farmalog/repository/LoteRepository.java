package br.com.farmalog.repository;

import br.com.farmalog.entity.Lote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {

	boolean existsByProdutoIdAndCodigo(Long produtoId, String codigo);

	Page<Lote> findByProdutoId(Long produtoId, Pageable pageable);

	@Query(value = """
			SELECT l FROM Lote l JOIN FETCH l.produto
			WHERE l.quantidadeAtual > 0 AND l.dataValidade BETWEEN :hoje AND :limite
			""",
			countQuery = """
			SELECT count(l) FROM Lote l
			WHERE l.quantidadeAtual > 0 AND l.dataValidade BETWEEN :hoje AND :limite
			""")
	Page<Lote> vencendoEntre(@Param("hoje") LocalDate hoje,
			@Param("limite") LocalDate limite,
			Pageable pageable);

	@Query("""
			SELECT l FROM Lote l JOIN FETCH l.produto
			WHERE l.quantidadeAtual > 0 AND l.dataValidade BETWEEN :hoje AND :limite
			ORDER BY l.dataValidade
			""")
	List<Lote> vencendoAte(@Param("hoje") LocalDate hoje, @Param("limite") LocalDate limite);

	@Query("""
			SELECT COALESCE(SUM(l.quantidadeAtual), 0) FROM Lote l
			WHERE l.produto.id = :produtoId AND l.quantidadeAtual > 0 AND l.dataValidade >= :hoje
			""")
	int disponivelPara(@Param("produtoId") Long produtoId, @Param("hoje") LocalDate hoje);
}
