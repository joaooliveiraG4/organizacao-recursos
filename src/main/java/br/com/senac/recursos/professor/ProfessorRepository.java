package br.com.senac.recursos.professor;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Professor> findAllByOrderByCodigoAsc();
}
