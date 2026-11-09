package br.com.senac.recursos.auditoria;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {

    List<EventoAuditoria> findByTipoAndRegistroIdOrderByOcorridoEmAscSequenciaAsc(String tipo, UUID registroId);
}
