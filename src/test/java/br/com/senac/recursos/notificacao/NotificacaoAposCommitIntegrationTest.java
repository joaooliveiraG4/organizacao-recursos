package br.com.senac.recursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * RF-11 com a aplicação inteira, PostgreSQL real e provedor simulado. Prova o
 * cenário da utility tree: com o provedor travado, quem aprova recebe resposta
 * em menos de 2 s, a mudança já confirmada não é desfeita e a falha fica no log.
 */
@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class NotificacaoAposCommitIntegrationTest {

    private static final Duration META_DE_RESPOSTA = Duration.ofSeconds(2);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @RegisterExtension
    static final WireMockExtension PROVEDOR = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private TransactionTemplate transacao;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("notificacao.url", () -> PROVEDOR.baseUrl() + "/notificacoes");
        // Limite maior que a meta de 2 s de propósito: se o envio não fosse em
        // segundo plano, quem aprova esperaria 3 s e o teste reprovaria.
        registry.add("notificacao.timeout", () -> "3s");
    }

    private static Notificacao novaAprovacao() {
        return new Notificacao(UUID.randomUUID(), TipoNotificacao.RESERVA_APROVADA, UUID.randomUUID());
    }

    @Test
    void enviaDepoisQueATransacaoConfirma() {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));

        transacao.executeWithoutResult(status -> eventos.publishEvent(novaAprovacao()));

        assertThat(aguardar(() -> PROVEDOR.findAll(postRequestedFor(urlEqualTo("/notificacoes"))).size() == 1)).isTrue();
    }

    @Test
    void naoEnviaQuandoATransacaoEDesfeita() throws InterruptedException {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));

        transacao.executeWithoutResult(status -> {
            eventos.publishEvent(novaAprovacao());
            status.setRollbackOnly();
        });

        Thread.sleep(500);
        assertThat(PROVEDOR.findAll(postRequestedFor(urlEqualTo("/notificacoes")))).isEmpty();
    }

    @Test
    void provedorTravadoNaoAtrasaQuemAprovaEFalhaFicaRegistrada(CapturedOutput log) {
        PROVEDOR.stubFor(post("/notificacoes")
                .willReturn(aResponse().withStatus(202).withFixedDelay(5_000)));
        var notificacao = novaAprovacao();

        long inicio = System.nanoTime();
        transacao.executeWithoutResult(status -> eventos.publishEvent(notificacao));
        var respostaParaQuemAprova = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(respostaParaQuemAprova).isLessThan(META_DE_RESPOSTA);
        assertThat(aguardar(() -> log.getOut().contains(
                "referencia=" + notificacao.referenciaId() + " resultado=TIMEOUT"))).isTrue();
    }

    private static boolean aguardar(BooleanSupplier condicao) {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condicao.getAsBoolean()) {
            if (System.nanoTime() > limite) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        return true;
    }
}
