package br.com.senac.recursos.status;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final Clock clock;

    public StatusController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "service", "organizacao-recursos",
                "status", "UP",
                "timestamp", Instant.now(clock));
    }
}

