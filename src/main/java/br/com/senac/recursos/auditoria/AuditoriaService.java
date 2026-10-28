package br.com.senac.recursos.auditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico auditável (RF-10, ADR 0003).
 *
 * <p>Uso esperado dentro da mesma transação da mudança de estado:
 * <pre>
 * auditoria.registrar("RESERVA", reserva.getId(), "RESERVA_APROVADA", usuario.id(),
 *         Map.of("de", "SOLICITADA", "para", "APROVADA"));
 * </pre>
 *
 * <p>Os detalhes não devem conter dados pessoais (nome, e-mail, telefone):
 * identifique pessoas somente por id, porque o evento não pode ser alterado depois.
 */
@Service
public class AuditoriaService {

    /** Autor técnico das ações executadas automaticamente pela aplicação. */
    public static final UUID AUTOR_SISTEMA = new UUID(0L, 0L);

    private final EventoAuditoriaRepository repository;
    private final Clock clock;

    AuditoriaService(EventoAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Exige uma transação aberta pelo chamador: a mudança e o evento são
     * confirmados ou desfeitos juntos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, UUID registroId, String acao, UUID autorId, Map<String, Object> detalhes) {
        exigirTexto(tipo, "Informe o tipo do registro auditado");
        Objects.requireNonNull(registroId, "Informe o registro auditado");
        exigirTexto(acao, "Informe a ação auditada");
        Objects.requireNonNull(autorId, "Informe o autor da ação");

        repository.save(new EventoAuditoria(
                tipo.trim(),
                registroId,
                acao.trim(),
                autorId,
                Instant.now(clock),
                detalhes == null ? Map.of() : Map.copyOf(detalhes)));
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> historico(String tipo, UUID registroId) {
        return repository.findByTipoAndRegistroIdOrderByOcorridoEmAscSequenciaAsc(tipo, registroId).stream()
                .map(EventoAuditoria::paraRegistro)
                .toList();
    }

    private static void exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
