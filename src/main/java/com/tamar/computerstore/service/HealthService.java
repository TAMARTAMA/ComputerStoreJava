package com.tamar.computerstore.service;

import com.tamar.computerstore.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {

    private final String applicationName;

    public HealthService(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    public HealthResponse getHealth() {
        return new HealthResponse("UP", applicationName, Instant.now());
    }
}
