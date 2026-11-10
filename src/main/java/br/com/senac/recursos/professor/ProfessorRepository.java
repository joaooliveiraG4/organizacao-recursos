package br.com.senac.recursos.professor;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    @Query("""
            select professor from Professor professor
            where (coalesce(:competencia, '') = ''
                or lower(professor.competencia) like lower(concat('%', :competencia, '%')))
            order by professor.codigo
            """)
    List<Professor> buscarPorCompetencia(@Param("competencia") String competencia);
}
