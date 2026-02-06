package com.course.controller;

import com.course.dto.NotificationDto;
import com.course.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationDto>> listMy() {
        return ResponseEntity.ok(notificationService.listMyNotifications());
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.countMyUnread()));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PatchMapping("/api/notifications/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllRead()));
    }
}
