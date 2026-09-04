package br.com.farmalog.repository;

import br.com.farmalog.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

	boolean existsByCodigoBarras(String codigoBarras);

	boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);
}
