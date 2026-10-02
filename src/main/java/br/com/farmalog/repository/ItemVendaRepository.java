package br.com.farmalog.repository;

import br.com.farmalog.entity.ItemVenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

	List<ItemVenda> findByVendaId(Long vendaId);

	List<ItemVenda> findByVendaIdIn(Collection<Long> vendaIds);

	@Query("""
			SELECT i.produto.id, i.produto.nome, SUM(i.quantidade) FROM ItemVenda i
			WHERE i.venda.status = br.com.farmalog.entity.StatusVenda.CONCLUIDA
			GROUP BY i.produto.id, i.produto.nome
			ORDER BY SUM(i.quantidade) DESC, i.produto.nome
			""")
	List<Object[]> maisVendidos(Pageable pageable);
}
