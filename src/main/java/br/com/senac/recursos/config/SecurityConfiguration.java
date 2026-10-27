package br.com.senac.recursos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.senac.recursos.seguranca.Perfil;
import br.com.senac.recursos.seguranca.RespostaAcessoNegado;

/**
 * Autorização negada por padrão (ADR 0002). Escrita no catálogo de recursos
 * é exclusiva do Administrador; consultas exigem apenas autenticação.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final String[] CATALOGO = {
        "/api/v1/salas/**", "/api/v1/professores/**", "/api/v1/materiais/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var respostaNegada = new RespostaAcessoNegado(objectMapper);
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/api/v1/status").permitAll()
                        .requestMatchers(HttpMethod.GET, CATALOGO).authenticated()
                        .requestMatchers(CATALOGO).hasRole(Perfil.ADMINISTRADOR.name())
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(respostaNegada))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(respostaNegada)
                        .accessDeniedHandler(respostaNegada))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
