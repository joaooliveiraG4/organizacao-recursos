package br.com.senac.recursos.auditoria;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RegistroAuditoria(
        UUID id,
        String tipo,
        UUID registroId,
        String acao,
        UUID autorId,
        Instant ocorridoEm,
        Map<String, Object> detalhes) {
}
