package com.workly.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        log.debug("HealthController: health check called");
        return Map.of("status", "UP", "message", "Workly Server is running fine");
    }
}
