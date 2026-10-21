package br.com.senac.recursos.sala;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class SalaExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(SalaJaCadastradaException.class)
    ProblemDetail salaDuplicada(SalaJaCadastradaException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(SalaNaoEncontradaException.class)
    ProblemDetail salaNaoEncontrada(SalaNaoEncontradaException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflitoDeIntegridade(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Não foi possível concluir a operação porque os dados conflitam com um registro existente.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
                status,
                "Os dados enviados para a sala são inválidos.");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "campo", error.getField(),
                        "mensagem", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("erros", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }
}
