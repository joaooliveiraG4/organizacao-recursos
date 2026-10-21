package br.com.senac.recursos.sala;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Sala> findAllByOrderByCodigoAsc();
}
