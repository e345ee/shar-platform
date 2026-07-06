package com.course.repository;

import com.course.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {

    List<Achievement> findAllByCourse_IdOrderByCreatedAtDesc(Integer courseId);

    boolean existsByCourse_IdAndTitleIgnoreCase(Integer courseId, String title);

    Optional<Achievement> findByIdAndCreatedBy_Id(Integer id, Integer createdById);
}
