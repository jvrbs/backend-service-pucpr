package br.pucpr.authserver;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    public record HealthResponse(String status) { }

    @GetMapping("/health")
    @Operation(summary = "Verificar disponibilidade", tags = "Health")
    public HealthResponse health() { return new HealthResponse("OK"); }
}
