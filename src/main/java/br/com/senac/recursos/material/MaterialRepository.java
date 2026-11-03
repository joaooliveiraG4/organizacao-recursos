package br.com.senac.recursos.material;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Material> findAllByOrderByCodigoAsc();
}
