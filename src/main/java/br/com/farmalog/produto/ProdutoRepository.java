package br.com.farmalog.produto;

import br.com.farmalog.produto.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProdutoRepository extends JpaRepository<Produto, Long> {

	boolean existsByCodigoBarras(String codigoBarras);

	boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);
}
