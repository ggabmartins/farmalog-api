package br.com.farmalog.repository;

import br.com.farmalog.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {
}