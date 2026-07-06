package com.course.repository;

import com.course.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findAllByUser_IdOrderByCreatedAtDesc(Integer userId);
    Page<Notification> findAllByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    long countByUser_IdAndReadFalse(Integer userId);
    Optional<Notification> findByIdAndUser_Id(Integer id, Integer userId);
}
