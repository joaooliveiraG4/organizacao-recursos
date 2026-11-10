package br.com.senac.recursos.notificacao;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class NotificacaoConfiguration {

    @Bean
    ClienteNotificacao clienteNotificacao(
            @Value("${notificacao.url}") URI url,
            @Value("${notificacao.timeout}") Duration timeout) {
        return new ClienteNotificacao(url, timeout);
    }
}
