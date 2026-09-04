package br.com.farmalog.produto;

import br.com.farmalog.produto.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

	boolean existsByCodigoBarras(String codigoBarras);

	boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);
}
