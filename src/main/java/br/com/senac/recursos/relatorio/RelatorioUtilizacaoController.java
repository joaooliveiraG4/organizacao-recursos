package br.com.senac.recursos.relatorio;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF-12 — relatório de utilização. Somente Responsável e Administrador. */
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioUtilizacaoController {

    private final RelatorioUtilizacaoService relatorios;

    public RelatorioUtilizacaoController(RelatorioUtilizacaoService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/utilizacao")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public RelatorioUtilizacao utilizacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim) {
        return relatorios.gerar(inicio, fim);
    }
}
