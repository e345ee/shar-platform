package com.course.repository;

import com.course.entity.StudentAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, Integer> {

    boolean existsByStudent_IdAndAchievement_Id(Integer studentId, Integer achievementId);

    Optional<StudentAchievement> findByStudent_IdAndAchievement_Id(Integer studentId, Integer achievementId);

    List<StudentAchievement> findAllByStudent_IdOrderByAwardedAtDesc(Integer studentId);

    List<StudentAchievement> findAllByAchievement_IdOrderByAwardedAtDesc(Integer achievementId);

    void deleteAllByAchievement_Id(Integer achievementId);
}
