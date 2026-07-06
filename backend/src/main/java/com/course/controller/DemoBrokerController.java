package com.course.controller;

import com.course.messaging.demo.DemoBrokerPublisher;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo")
@RequiredArgsConstructor
@Tag(name = "Demo broker", description = "Demo endpoints for broker integration")
@ConditionalOnProperty(prefix = "app.demo.broker", name = "enabled", havingValue = "true")
public class DemoBrokerController {

    private final DemoBrokerPublisher publisher;

    @PostMapping("/broker/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ping(@RequestParam(defaultValue = "ping") String message) {
        publisher.publishPing(message);
        return ResponseEntity.accepted().build();
    }
}
