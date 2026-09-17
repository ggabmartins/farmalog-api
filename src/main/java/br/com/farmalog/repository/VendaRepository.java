package br.com.farmalog.repository;

import br.com.farmalog.entity.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface VendaRepository extends JpaRepository<Venda, Long> {

	@Query(value = """
			SELECT v FROM Venda v JOIN FETCH v.operador
			WHERE (:operadorId IS NULL OR v.operador.id = :operadorId)
			  AND (CAST(:inicio AS timestamp) IS NULL OR v.dataHora >= :inicio)
			  AND (CAST(:fim AS timestamp) IS NULL OR v.dataHora < :fim)
			""",
			countQuery = """
			SELECT count(v) FROM Venda v
			WHERE (:operadorId IS NULL OR v.operador.id = :operadorId)
			  AND (CAST(:inicio AS timestamp) IS NULL OR v.dataHora >= :inicio)
			  AND (CAST(:fim AS timestamp) IS NULL OR v.dataHora < :fim)
			""")
	Page<Venda> buscar(@Param("operadorId") Long operadorId,
			@Param("inicio") Instant inicio,
			@Param("fim") Instant fim,
			Pageable pageable);
}
