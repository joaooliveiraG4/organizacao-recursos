package br.com.senac.recursos.material;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    @Query("""
            select material from Material material
            where (coalesce(:tipo, '') = ''
                or lower(material.tipo) like lower(concat('%', :tipo, '%')))
            order by material.codigo
            """)
    List<Material> buscarPorTipo(@Param("tipo") String tipo);
}
