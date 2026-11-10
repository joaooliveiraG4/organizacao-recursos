package br.com.senac.recursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RF-11 — o provedor externo é simulado com WireMock. Risco coberto: provedor
 * lento ou com erro degradar o sistema. Em todos os casos o cliente devolve um
 * resultado controlado, sem exceção e sem esperar além do limite.
 */
class ClienteNotificacaoTest {

    /** Mesmo limite da configuração padrão (application.yml). */
    private static final Duration LIMITE = Duration.ofSeconds(2);
    /** Limite curto só para provar o timeout sem deixar o teste lento. */
    private static final Duration LIMITE_CURTO = Duration.ofMillis(500);
    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @RegisterExtension
    static final WireMockExtension PROVEDOR = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ClienteNotificacao cliente() {
        return cliente(LIMITE);
    }

    private ClienteNotificacao cliente(Duration limite) {
        return new ClienteNotificacao(URI.create(PROVEDOR.baseUrl() + "/notificacoes"), limite);
    }

    private static Notificacao aprovacaoDaAna() {
        return new Notificacao(ANA, TipoNotificacao.RESERVA_APROVADA, RESERVA);
    }

    @Test
    void enviaSomenteIdsEConfirmaQuandoProvedorAceita() {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));

        assertThat(cliente().enviar(aprovacaoDaAna())).isEqualTo(ResultadoEnvio.ENVIADA);

        PROVEDOR.verify(postRequestedFor(urlEqualTo("/notificacoes")).withRequestBody(equalToJson("""
                {"destinatarioId": "11111111-1111-1111-1111-111111111111",
                 "tipo": "RESERVA_APROVADA",
                 "referenciaId": "22222222-2222-2222-2222-222222222222"}
                """)));
    }

    @Test
    void desisteNoLimiteQuandoProvedorDemora() {
        // Aquecimento: a 1ª requisição carrega e instrumenta (JaCoCo) as classes
        // do cliente e do WireMock; isso não faz parte do tempo do nosso timeout.
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));
        cliente().enviar(aprovacaoDaAna());
        PROVEDOR.resetAll();

        PROVEDOR.stubFor(post("/notificacoes")
                .willReturn(aResponse().withStatus(202).withFixedDelay(5_000)));

        long inicio = System.nanoTime();
        var resultado = cliente(LIMITE_CURTO).enviar(aprovacaoDaAna());
        var duracao = Duration.ofNanos(System.nanoTime() - inicio);

        // Provedor travado por 5 s; o cliente desiste antes da meta de 2 s
        // (margem acima de 500 ms para máquinas de CI lentas).
        assertThat(resultado).isEqualTo(ResultadoEnvio.TIMEOUT);
        assertThat(duracao).isLessThan(Duration.ofSeconds(2));
    }

    /** Só 2xx é entrega: redirecionamento (3xx) também não confirma o aviso. */
    @ParameterizedTest(name = "provedor responde {0}")
    @ValueSource(ints = {301, 302, 400, 500, 503})
    void respostaDeErroViraResultadoControlado(int status) {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(status)));

        assertThat(cliente().enviar(aprovacaoDaAna())).isEqualTo(ResultadoEnvio.ERRO_DO_PROVEDOR);
    }

    @Test
    void provedorForaDoArViraIndisponivel() throws IOException {
        int portaFechada;
        try (var socket = new ServerSocket(0)) {
            portaFechada = socket.getLocalPort();
        }
        var semProvedor = new ClienteNotificacao(
                URI.create("http://localhost:" + portaFechada + "/notificacoes"), LIMITE);

        assertThat(semProvedor.enviar(aprovacaoDaAna())).isEqualTo(ResultadoEnvio.INDISPONIVEL);
    }
}
