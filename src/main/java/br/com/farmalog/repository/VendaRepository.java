package br.com.farmalog.repository;

import br.com.farmalog.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaRepository extends JpaRepository<Venda, Long> {
}
