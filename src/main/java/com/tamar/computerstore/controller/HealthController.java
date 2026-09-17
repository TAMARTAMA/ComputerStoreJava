package com.tamar.computerstore.controller;

import com.tamar.computerstore.dto.HealthResponse;
import com.tamar.computerstore.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Legacy application health (kept for clients; prefer Actuator probes in ops)")
public class HealthController {

    private final HealthService healthService;

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(summary = "Application health", description = "Public backward-compatible health JSON. Ops should also use /actuator/health.")
    @ApiResponse(responseCode = "200", description = "UP",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(healthService.getHealth());
    }
}
