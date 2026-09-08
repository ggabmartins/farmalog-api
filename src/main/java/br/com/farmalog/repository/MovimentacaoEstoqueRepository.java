package br.com.farmalog.repository;

import br.com.farmalog.entity.MovimentacaoEstoque;
import br.com.farmalog.entity.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

	@Query(value = """
			SELECT m FROM MovimentacaoEstoque m
			JOIN FETCH m.lote l
			JOIN FETCH l.produto
			JOIN FETCH m.usuario
			WHERE (:tipo IS NULL OR m.tipo = :tipo)
			  AND (:loteId IS NULL OR l.id = :loteId)
			  AND (CAST(:inicio AS timestamp) IS NULL OR m.dataHora >= :inicio)
			  AND (CAST(:fim AS timestamp) IS NULL OR m.dataHora < :fim)
			""",
			countQuery = """
			SELECT count(m) FROM MovimentacaoEstoque m
			WHERE (:tipo IS NULL OR m.tipo = :tipo)
			  AND (:loteId IS NULL OR m.lote.id = :loteId)
			  AND (CAST(:inicio AS timestamp) IS NULL OR m.dataHora >= :inicio)
			  AND (CAST(:fim AS timestamp) IS NULL OR m.dataHora < :fim)
			""")
	Page<MovimentacaoEstoque> buscar(@Param("tipo") TipoMovimentacao tipo,
			@Param("loteId") Long loteId,
			@Param("inicio") Instant inicio,
			@Param("fim") Instant fim,
			Pageable pageable);
}
