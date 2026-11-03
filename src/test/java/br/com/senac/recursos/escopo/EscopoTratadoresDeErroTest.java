package br.com.senac.recursos.escopo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.senac.recursos.config.SecurityConfiguration;

/**
 * Tratadores de erro de um módulo não podem responder por outro: a validação
 * de um controller fora de sala/professor/material não recebe a mensagem deles.
 * O @WebMvcTest carrega todos os @RestControllerAdvice da aplicação.
 */
@WebMvcTest(EscopoTratadoresDeErroTest.ControllerDeOutroModulo.class)
@Import({SecurityConfiguration.class, EscopoTratadoresDeErroTest.ControllerDeOutroModulo.class})
@WithMockUser
class EscopoTratadoresDeErroTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validacaoDeOutroModuloNaoRecebeMensagemDoCatalogo() throws Exception {
        mockMvc.perform(post("/teste/outro-modulo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("sala"))))
                .andExpect(content().string(not(containsString("catálogo"))));
    }

    record Pedido(@NotBlank String nome) {
    }

    @RestController
    static class ControllerDeOutroModulo {

        @PostMapping("/teste/outro-modulo")
        void receber(@Valid @RequestBody Pedido pedido) {
            // Só existe para disparar a validação; o corpo inválido nunca chega aqui.
        }
    }
}
