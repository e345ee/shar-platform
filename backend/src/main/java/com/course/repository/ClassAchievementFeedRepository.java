package com.course.repository;

import com.course.entity.ClassAchievementFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassAchievementFeedRepository extends JpaRepository<ClassAchievementFeed, Integer> {

    @Query("select f from ClassAchievementFeed f " +
            "join fetch f.student s " +
            "join fetch f.awardedBy ab " +
            "join fetch f.achievement a " +
            "left join fetch a.course c " +
            "where f.studyClass.id = :classId " +
            "order by f.createdAt desc")
    List<ClassAchievementFeed> findFeedByClassId(@Param("classId") Integer classId);
}
