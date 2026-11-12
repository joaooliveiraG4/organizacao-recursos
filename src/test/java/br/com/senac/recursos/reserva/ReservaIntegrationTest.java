package br.com.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class ReservaIntegrationTest {

    private static final Instant INICIO = Instant.parse("2026-11-12T14:00:00Z");
    private static final Instant FIM = Instant.parse("2026-11-12T16:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private EntityManager entidades;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder senhas;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void gravaEReleReservaSolicitada() {
        var solicitante = cadastrarSolicitante();
        var periodo = new PeriodoReserva(INICIO, FIM);
        var reserva = new Reserva(solicitante, periodo);

        entidades.persist(reserva);
        entidades.flush();
        entidades.clear();

        var gravada = entidades.find(Reserva.class, reserva.getId());
        assertThat(gravada.getSolicitanteId()).isEqualTo(solicitante);
        assertThat(gravada.getPeriodo()).isEqualTo(periodo);
        assertThat(gravada.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    void bancoRecusaPeriodoVazioMesmoSemPassarPeloDominio() {
        var solicitante = cadastrarSolicitante();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO reserva (id, solicitante_id, inicio, fim)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), solicitante, INICIO.atOffset(ZoneOffset.UTC),
                INICIO.atOffset(ZoneOffset.UTC)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reserva_periodo");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void apenasUmaTransacaoConsegueReservarAMesmaSalaNoMesmoHorario() throws Exception {
        var solicitante = cadastrarSolicitante();
        var sala = UUID.randomUUID();
        var primeiraGravou = new CountDownLatch(1);
        var segundaVaiGravar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> tentarReservarSala(solicitante, sala, () -> {}, () -> {
                primeiraGravou.countDown();
                aguardarInicio(segundaVaiGravar);
            }));
            var segunda = executor.submit(() -> tentarReservarSala(solicitante, sala, () -> {
                aguardarInicio(primeiraGravou);
                segundaVaiGravar.countDown();
            }, () -> {}));
            assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("COMMIT", "23P01");
        }

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva_recurso
                WHERE tipo_recurso = 'SALA' AND recurso_id = ? AND ativo
                """, Long.class, sala)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva WHERE solicitante_id = ?
                """, Long.class, solicitante)).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void apiAceitaUmaDasDuasReservasSimultaneasParaAMesmaSala() throws Exception {
        var senha = UUID.randomUUID().toString();
        var primeiro = cadastrarSolicitante(senhas.encode(senha));
        var segundo = cadastrarSolicitante(senhas.encode(senha));
        var sala = cadastrarSala(false);
        var pedido = pedidoPara(sala);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> postarReserva(primeiro, senha, pedido, prontas, iniciar));
            var segunda = executor.submit(() -> postarReserva(segundo, senha, pedido, prontas, iniciar));
            boolean ambasProntas = prontas.await(10, TimeUnit.SECONDS);
            iniciar.countDown();
            assertThat(ambasProntas).isTrue();
            assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva_recurso
                WHERE tipo_recurso = 'SALA' AND recurso_id = ? AND ativo
                """, Long.class, sala)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM reserva WHERE solicitante_id IN (?, ?)
                """, Long.class, primeiro, segundo)).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void apiRecusaSobreposicaoDoMesmoProfessorEmSalasDiferentes() throws Exception {
        var senha = UUID.randomUUID().toString();
        var solicitante = cadastrarSolicitante(senhas.encode(senha));
        var professor = cadastrarProfessor();
        var primeiraSala = cadastrarSala(false);
        var segundaSala = cadastrarSala(false);
        var primeiraReserva = pedidoComProfessor(primeiraSala, professor);
        var segundaReserva = pedidoComProfessor(segundaSala, professor);
        criarPelaApi(solicitante, senha, primeiraReserva);

        mvc.perform(post("/api/v1/reservas")
                        .with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(segundaReserva))
                .andExpect(status().isConflict());

        assertThat(alocacoesAtivas("PROFESSOR", professor)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE solicitante_id = ?",
                Long.class, solicitante)).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void apiRecusaSobreposicaoDoMesmoMaterialEmSalasDiferentes() throws Exception {
        var senha = UUID.randomUUID().toString();
        var solicitante = cadastrarSolicitante(senhas.encode(senha));
        var material = cadastrarMaterial();
        var primeiraSala = cadastrarSala(false);
        var segundaSala = cadastrarSala(false);
        var primeiraReserva = pedidoComMaterial(primeiraSala, material);
        var segundaReserva = pedidoComMaterial(segundaSala, material);
        criarPelaApi(solicitante, senha, primeiraReserva);

        mvc.perform(post("/api/v1/reservas")
                        .with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(segundaReserva))
                .andExpect(status().isConflict());

        assertThat(alocacoesAtivas("MATERIAL", material)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE solicitante_id = ?",
                Long.class, solicitante)).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void edicaoRestritaVoltaParaSolicitadaECancelamentoLiberaHorario() throws Exception {
        var senha = UUID.randomUUID().toString();
        var solicitante = cadastrarSolicitante(senhas.encode(senha));
        var sala = cadastrarSala(true);
        var pedido = pedidoPara(sala);
        var id = criarPelaApi(solicitante, senha, pedido);
        definirEstado(id, EstadoReserva.APROVADA);

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SOLICITADA"));
        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA", "RESERVA_EDITADA");

        mvc.perform(delete("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, id))
                .isEqualTo("CANCELADA");
        assertThat(jdbc.queryForObject("SELECT ativo FROM reserva_recurso WHERE reserva_id = ?",
                Boolean.class, id)).isFalse();
        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA", "RESERVA_EDITADA", "RESERVA_CANCELADA");
        criarPelaApi(solicitante, senha, pedido);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"SOLICITADA", "APROVADA"}, mode = EnumSource.Mode.EXCLUDE)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void estadosNaoEditaveisRecusamEdicaoECancelamento(EstadoReserva estado) throws Exception {
        var senha = UUID.randomUUID().toString();
        var solicitante = cadastrarSolicitante(senhas.encode(senha));
        var pedido = pedidoPara(cadastrarSala(false));
        var id = criarPelaApi(solicitante, senha, pedido);
        definirEstado(id, estado);

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha)))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, id))
                .isEqualTo(estado.name());
        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void conflitoDeEdicaoPreservaAlocacaoEEstadoAnteriores() throws Exception {
        var senha = UUID.randomUUID().toString();
        var solicitante = cadastrarSolicitante(senhas.encode(senha));
        var salaOriginal = cadastrarSala(false);
        var id = criarPelaApi(solicitante, senha, pedidoPara(salaOriginal));
        definirEstado(id, EstadoReserva.APROVADA);
        var pedidoOcupado = pedidoPara(cadastrarSala(true));
        criarPelaApi(solicitante, senha, pedidoOcupado);

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedidoOcupado))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT recurso_id FROM reserva_recurso WHERE reserva_id = ? AND ativo",
                UUID.class, id)).isEqualTo(salaOriginal);
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, id))
                .isEqualTo("APROVADA");
        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA");

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedidoPara(cadastrarSala(false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROVADA"));
        mvc.perform(delete("/api/v1/reservas/{id}", id).with(httpBasic(solicitante + "@example.test", senha)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void terceiroRecebe403EAdminEditaECancelaComAutoriaPropria() throws Exception {
        var senha = UUID.randomUUID().toString();
        var hash = senhas.encode(senha);
        var dono = cadastrarSolicitante(hash);
        var terceiro = cadastrarSolicitante(hash);
        var admin = cadastrarUsuario(hash, "ADMINISTRADOR");
        var pedido = pedidoPara(cadastrarSala(false));
        var id = criarPelaApi(dono, senha, pedido);

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(terceiro + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reservas/{id}", id).with(httpBasic(terceiro + "@example.test", senha)))
                .andExpect(status().isForbidden());
        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA");

        mvc.perform(put("/api/v1/reservas/{id}", id).with(httpBasic(admin + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/reservas/{id}", id).with(httpBasic(admin + "@example.test", senha)))
                .andExpect(status().isNoContent());

        assertThat(acoes(id)).containsExactly("RESERVA_CRIADA", "RESERVA_EDITADA", "RESERVA_CANCELADA");
        assertThat(jdbc.queryForList("""
                SELECT actor_id FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, UUID.class, id)).containsExactly(dono, admin, admin);
        assertThat(jdbc.queryForObject("SELECT solicitante_id FROM reserva WHERE id = ?", UUID.class, id))
                .isEqualTo(dono);
    }

    private UUID criarPelaApi(UUID solicitante, String senha, String pedido) throws Exception {
        var resposta = mvc.perform(post("/api/v1/reservas")
                        .with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String id = JsonPath.read(resposta.getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    private UUID cadastrarProfessor() {
        var professor = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO professor (id, codigo, nome, competencia)
                VALUES (?, ?, 'Professor de teste', 'Matemática')
                """, professor, professor.toString().substring(0, 20));
        return professor;
    }

    private UUID cadastrarMaterial() {
        var material = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO material (id, codigo, nome, tipo, quantidade)
                VALUES (?, ?, 'Material de teste', 'Equipamento', 1)
                """, material, material.toString().substring(0, 20));
        return material;
    }

    private long alocacoesAtivas(String tipo, UUID recurso) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM reserva_recurso
                WHERE tipo_recurso = ? AND recurso_id = ? AND ativo
                """, Long.class, tipo, recurso);
    }

    private static String pedidoComProfessor(UUID sala, UUID professor) {
        return """
                {"salaId":"%s","professorId":"%s","inicio":"%s","fim":"%s"}
                """.formatted(sala, professor, INICIO, FIM);
    }

    private static String pedidoComMaterial(UUID sala, UUID material) {
        return """
                {"salaId":"%s","materialIds":["%s"],"inicio":"%s","fim":"%s"}
                """.formatted(sala, material, INICIO, FIM);
    }
    private UUID cadastrarSala(boolean restrita) {
        var sala = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sala (id, codigo, tipo, capacidade, localizacao, restrito)
                VALUES (?, ?, 'Aula', 20, 'Teste', ?)
                """, sala, sala.toString().substring(0, 20), restrita);
        return sala;
    }

    private static String pedidoPara(UUID sala) {
        return """
                {"salaId":"%s","inicio":"%s","fim":"%s"}
                """.formatted(sala, INICIO, FIM);
    }

    private void definirEstado(UUID id, EstadoReserva estado) {
        jdbc.update("UPDATE reserva SET estado = ? WHERE id = ?", estado.name(), id);
    }

    private List<String> acoes(UUID id) {
        return jdbc.queryForList("""
                SELECT action FROM audit_event WHERE aggregate_id = ? ORDER BY sequence_number
                """, String.class, id);
    }

    private int postarReserva(UUID solicitante, String senha, String pedido, CountDownLatch prontas,
            CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        aguardarInicio(iniciar);
        return mvc.perform(post("/api/v1/reservas")
                        .with(httpBasic(solicitante + "@example.test", senha))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andReturn().getResponse().getStatus();
    }

    private String tentarReservarSala(UUID solicitante, UUID sala, Runnable antesDaAlocacao,
            Runnable depoisDaAlocacao) {
        try {
            transacao.executeWithoutResult(status -> {
                var reserva = new Reserva(solicitante, new PeriodoReserva(INICIO, FIM));
                entidades.persist(reserva);
                entidades.flush();
                jdbc.execute("SET LOCAL statement_timeout = '10s'");
                antesDaAlocacao.run();
                jdbc.update("""
                        INSERT INTO reserva_recurso (reserva_id, tipo_recurso, recurso_id, periodo)
                        VALUES (?, 'SALA', ?, tstzrange(?, ?, '[)'))
                        """, reserva.getId(), sala, INICIO.atOffset(ZoneOffset.UTC),
                        FIM.atOffset(ZoneOffset.UTC));
                depoisDaAlocacao.run();
            });
            return "COMMIT";
        } catch (DataIntegrityViolationException conflito) {
            if (conflito.getMostSpecificCause() instanceof SQLException sql
                    && "23P01".equals(sql.getSQLState())) {
                return sql.getSQLState();
            }
            throw conflito;
        }
    }

    private static void aguardarInicio(CountDownLatch iniciar) {
        try {
            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As transações não foram liberadas a tempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A disputa foi interrompida", e);
        }
    }

    private UUID cadastrarSolicitante() {
        return cadastrarSolicitante("!sem-senha");
    }

    private UUID cadastrarSolicitante(String senhaHash) {
        return cadastrarUsuario(senhaHash, "SOLICITANTE");
    }

    private UUID cadastrarUsuario(String senhaHash, String perfil) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user (id, name, email, role, password_hash)
                VALUES (?, ?, ?, ?, ?)
                """, id, "Solicitante de teste", id + "@example.test", perfil, senhaHash);
        return id;
    }
}
