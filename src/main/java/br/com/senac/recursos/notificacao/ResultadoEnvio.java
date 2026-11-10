package br.com.senac.recursos.notificacao;

/** Como terminou uma tentativa de envio. Nenhum resultado lança exceção. */
public enum ResultadoEnvio {
    /** O provedor respondeu 2xx. */
    ENVIADA,
    /** O provedor não respondeu dentro do tempo limite. */
    TIMEOUT,
    /** O provedor respondeu fora de 2xx (redirecionamento 3xx, 4xx ou 5xx). */
    ERRO_DO_PROVEDOR,
    /** Não foi possível conectar ao provedor. */
    INDISPONIVEL
}
