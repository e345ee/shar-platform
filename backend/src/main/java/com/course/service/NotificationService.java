package com.course.service;

import com.course.dto.NotificationDto;
import com.course.entity.Notification;
import com.course.entity.NotificationType;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.NotificationNotFoundException;
import com.course.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthService authService;

    public NotificationDto create(User recipient, NotificationType type, String title, String message,
                                  Integer courseId, Integer classId, Integer testId, Integer attemptId, Integer achievementId) {
        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        Notification n = new Notification();
        n.setUser(recipient);
        n.setType(type);
        n.setTitle(title == null ? "" : title.trim());
        n.setMessage(message);
        n.setCourseId(courseId);
        n.setClassId(classId);
        n.setTestId(testId);
        n.setAttemptId(attemptId);
        n.setAchievementId(achievementId);
        n.setRead(false);
        return toDto(notificationRepository.save(n));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listMyNotifications() {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        return notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(current.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countMyUnread() {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        return notificationRepository.countByUser_IdAndReadFalse(current.getId());
    }

    public NotificationDto markRead(Integer notificationId) {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        Notification n = notificationRepository.findByIdAndUser_Id(notificationId, current.getId())
                .orElseThrow(() -> new NotificationNotFoundException("Notification with id " + notificationId + " not found"));
        n.setRead(true);
        return toDto(notificationRepository.save(n));
    }

    public int markAllRead() {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        List<Notification> all = notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(current.getId());
        int changed = 0;
        for (Notification n : all) {
            if (!n.isRead()) {
                n.setRead(true);
                changed++;
            }
        }
        if (changed > 0) {
            notificationRepository.saveAll(all);
        }
        return changed;
    }

    public NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType() != null ? n.getType().name() : null);
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setRead(n.isRead());
        dto.setCourseId(n.getCourseId());
        dto.setClassId(n.getClassId());
        dto.setTestId(n.getTestId());
        dto.setAttemptId(n.getAttemptId());
        dto.setAchievementId(n.getAchievementId());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
