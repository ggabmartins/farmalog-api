package br.com.farmalog.repository;

import br.com.farmalog.entity.ItemVenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

	List<ItemVenda> findByVendaId(Long vendaId);

	List<ItemVenda> findByVendaIdIn(Collection<Long> vendaIds);
}
