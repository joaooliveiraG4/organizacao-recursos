package br.com.senac.recursos.sala;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalaRepository extends JpaRepository<Sala, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    @Query("""
            select sala from Sala sala
            where (coalesce(:tipo, '') = ''
                or lower(sala.tipo) like lower(concat('%', :tipo, '%')))
              and (coalesce(:capacidadeMinima, 0) = 0
                or sala.capacidade >= :capacidadeMinima)
              and (coalesce(:localizacao, '') = ''
                or lower(sala.localizacao) like lower(concat('%', :localizacao, '%')))
            order by sala.codigo
            """)
    List<Sala> buscar(
            @Param("tipo") String tipo,
            @Param("capacidadeMinima") Integer capacidadeMinima,
            @Param("localizacao") String localizacao);
}
