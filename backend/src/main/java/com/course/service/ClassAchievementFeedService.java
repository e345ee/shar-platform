package com.course.service;

import com.course.dto.achievement.StudentAchievementResponse;
import com.course.dto.common.PageResponse;
import com.course.entity.*;
import com.course.repository.ClassAchievementFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClassAchievementFeedService {

    private final ClassAchievementFeedRepository feedRepository;

    
    private final ClassStudentService classStudentService;
    private final StudyClassService studyClassService;

    
    @Transactional
    public void publishAward(StudentAchievement awarded) {
        if (awarded == null || awarded.getId() == null) {
            return;
        }

        User student = awarded.getStudent();
        Achievement achievement = awarded.getAchievement();
        User teacher = awarded.getAwardedBy();

        if (student == null || student.getId() == null || achievement == null || achievement.getId() == null) {
            return;
        }
        if (achievement.getCourse() == null || achievement.getCourse().getId() == null) {
            return;
        }

        Integer courseId = achievement.getCourse().getId();
        List<Integer> classIds = classStudentService.findClassIdsByStudentInCourse(student.getId(), courseId);
        if (classIds == null || classIds.isEmpty()) {
            return;
        }

        List<ClassAchievementFeed> batch = new ArrayList<>();
        for (Integer classId : classIds) {
            if (classId == null) continue;
            StudyClass studyClass = studyClassService.getEntityById(classId);

            ClassAchievementFeed f = new ClassAchievementFeed();
            f.setStudyClass(studyClass);
            f.setStudent(student);
            f.setAchievement(achievement);
            f.setAwardedBy(teacher);
            f.setAwardedAt(awarded.getAwardedAt());
            batch.add(f);
        }

        if (!batch.isEmpty()) {
            feedRepository.saveAll(batch);
        }
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementResponse> getFeedForClass(Integer classId) {
        List<ClassAchievementFeed> feed = feedRepository.findFeedByClassId(classId);
        return feed.stream().map(this::toDto).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentAchievementResponse> getFeedForClass(Integer classId, Pageable pageable) {
        List<StudentAchievementResponse> all = getFeedForClass(classId);
        int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : all.size();
        if (pageSize <= 0) {
            pageSize = all.size() == 0 ? 1 : all.size();
        }
        int from = Math.min(pageNumber * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<StudentAchievementResponse> content = all.subList(from, to);
        int totalPages = (int) Math.ceil(all.size() / (double) pageSize);
        boolean first = pageNumber <= 0;
        boolean last = pageNumber >= Math.max(0, totalPages - 1);

        return new PageResponse<>(content, pageNumber, pageSize, all.size(), totalPages, last, first);
    }

    private StudentAchievementResponse toDto(ClassAchievementFeed f) {
        if (f == null) return null;
        StudentAchievementResponse dto = new StudentAchievementResponse();
        
        dto.setId(f.getId());
        dto.setAwardedAt(f.getAwardedAt());

        if (f.getStudent() != null) {
            dto.setStudentId(f.getStudent().getId());
            dto.setStudentName(f.getStudent().getName());
        }

        if (f.getAchievement() != null) {
            dto.setAchievementId(f.getAchievement().getId());
            dto.setAchievementTitle(f.getAchievement().getTitle());
            dto.setAchievementPhotoUrl(f.getAchievement().getPhotoUrl());
            dto.setAchievementJokeDescription(f.getAchievement().getJokeDescription());
            dto.setAchievementDescription(f.getAchievement().getDescription());
            if (f.getAchievement().getCourse() != null) {
                dto.setAchievementCourseId(f.getAchievement().getCourse().getId());
            }
        }

        if (f.getAwardedBy() != null) {
            dto.setAwardedById(f.getAwardedBy().getId());
            dto.setAwardedByName(f.getAwardedBy().getName());
        }

        return dto;
    }
}
