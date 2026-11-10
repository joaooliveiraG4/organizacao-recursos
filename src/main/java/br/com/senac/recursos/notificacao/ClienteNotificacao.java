package br.com.senac.recursos.notificacao;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RF-11 — fala com o serviço externo de notificação. Espera no máximo
 * {@code timeout} (conexão e resposta) e nunca lança exceção: qualquer falha
 * vira um {@link ResultadoEnvio}, para que o provedor não derrube o sistema.
 */
public class ClienteNotificacao {

    private final URI url;
    private final RestClient restClient;

    public ClienteNotificacao(URI url, Duration timeout) {
        this.url = url;
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public ResultadoEnvio enviar(Notificacao notificacao) {
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notificacao)
                    .retrieve()
                    .toBodilessEntity();
            return ResultadoEnvio.ENVIADA;
        } catch (RestClientResponseException erroHttp) {
            return ResultadoEnvio.ERRO_DO_PROVEDOR;
        } catch (ResourceAccessException erroDeAcesso) {
            return foiTimeout(erroDeAcesso) ? ResultadoEnvio.TIMEOUT : ResultadoEnvio.INDISPONIVEL;
        } catch (RestClientException outroErro) {
            return ResultadoEnvio.INDISPONIVEL;
        }
    }

    private static boolean foiTimeout(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
