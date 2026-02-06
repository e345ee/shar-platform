package com.course.controller;

import com.course.service.MailService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@Validated
public class EmailController {

    private final MailService mailService;

    @PostMapping("/api/emails/users/{userId}")
    public ResponseEntity<Map<String, String>> sendToUser(
            @PathVariable @NotNull Integer userId,
            @RequestParam("subject") @NotBlank @Size(max = 200) String subject,
            @RequestParam("text") @NotBlank @Size(max = 5000) String text
    ) {
        mailService.sendToUser(userId, subject, text);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }
}
