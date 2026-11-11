package br.com.senac.recursos.seguranca;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Respostas neutras para 401 e 403. A mensagem é a mesma para senha errada,
 * usuário inexistente ou inativo, para não revelar quais contas existem.
 */
public class RespostaAcessoNegado implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String NAO_AUTENTICADO = "Credenciais ausentes ou inválidas.";
    static final String SEM_PERMISSAO = "Seu perfil não tem permissão para esta operação.";

    private final ObjectMapper objectMapper;

    public RespostaAcessoNegado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"organizacao-recursos\"");
        escrever(response, HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, SEM_PERMISSAO);
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String detalhe) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
