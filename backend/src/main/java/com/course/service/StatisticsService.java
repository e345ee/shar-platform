package com.course.service;

import com.course.dto.*;
import com.course.entity.Course;
import com.course.entity.RoleName;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final RoleName ROLE_ADMIN = RoleName.ADMIN;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final AuthService authService;
    private final UserService userService;
    private final CourseService courseService;
    private final StudyClassService studyClassService;
    private final ClassStudentService classStudentService;

    private final StatisticsRepository statisticsRepository;

    public List<TeacherStatsDto> getTeacherStatsForCurrentMethodist(Integer methodistIdOverrideForAdmin) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_METHODIST, ROLE_ADMIN);

        Integer methodistId = null;
        if (isRole(current, ROLE_METHODIST)) {
            methodistId = current.getId();
        } else {
            // ADMIN may request stats for a specific methodist
            methodistId = methodistIdOverrideForAdmin;
        }

        if (methodistId == null) {
            return List.of();
        }

        return statisticsRepository.findTeacherStatsForMethodist(methodistId)
                .stream().map(p -> {
                    TeacherStatsDto dto = new TeacherStatsDto();
                    dto.setTeacherId(p.getTeacherId());
                    dto.setTeacherName(p.getTeacherName());
                    dto.setTeacherEmail(p.getTeacherEmail());
                    dto.setClassesCount(nullSafe(p.getClassesCount()));
                    dto.setStudentsCount(nullSafe(p.getStudentsCount()));
                    dto.setSubmittedAttemptsCount(nullSafe(p.getSubmittedAttemptsCount()));
                    dto.setGradedAttemptsCount(nullSafe(p.getGradedAttemptsCount()));
                    dto.setAvgGradePercent(p.getAvgGradePercent());
                    return dto;
                }).toList();
    }

    /**
     * CSV export for SRS 3.1.4.
     */
    public String exportTeacherStatsCsv(Integer methodistIdOverrideForAdmin) {
        List<TeacherStatsDto> rows = getTeacherStatsForCurrentMethodist(methodistIdOverrideForAdmin);

        StringBuilder sb = new StringBuilder();
        sb.append("teacherId,teacherName,teacherEmail,classesCount,studentsCount,submittedAttemptsCount,gradedAttemptsCount,avgGradePercent\n");

        for (TeacherStatsDto r : rows) {
            sb.append(csv(r.getTeacherId()));
            sb.append(',').append(csv(r.getTeacherName()));
            sb.append(',').append(csv(r.getTeacherEmail()));
            sb.append(',').append(csv(r.getClassesCount()));
            sb.append(',').append(csv(r.getStudentsCount()));
            sb.append(',').append(csv(r.getSubmittedAttemptsCount()));
            sb.append(',').append(csv(r.getGradedAttemptsCount()));
            sb.append(',').append(csv(r.getAvgGradePercent()));
            sb.append('\n');
        }
        return sb.toString();
    }

    public List<StudentTopicStatsDto> getMyTopicStats(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        if (courseId != null) {
            classStudentService.assertStudentInCourse(current.getId(), courseId, "Student is not enrolled in this course");
        }

        return statisticsRepository.findStudentTopicStats(current.getId(), courseId)
                .stream()
                .map(p -> {
                    StudentTopicStatsDto dto = new StudentTopicStatsDto();
                    dto.setCourseId(p.getCourseId());
                    dto.setCourseName(p.getCourseName());
                    dto.setTopic(p.getTopic());
                    dto.setTestsAttempted(p.getTestsAttempted());
                    dto.setGradedTests(p.getGradedTests());
                    dto.setAttemptsCount(p.getAttemptsCount());
                    dto.setGradedAttemptsCount(p.getGradedAttemptsCount());
                    dto.setAvgBestPercent(p.getAvgBestPercent());
                    return dto;
                })
                .toList();
    }

    public StudentStatisticsOverviewDto getMyOverview() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        StudentOverviewStatsProjection p = statisticsRepository.getStudentOverviewStats(current.getId());
        StudentStatisticsOverviewDto dto = new StudentStatisticsOverviewDto();
        dto.setAttemptsTotal(nullSafe(p.getAttemptsTotal()));
        dto.setAttemptsInProgress(nullSafe(p.getAttemptsInProgress()));
        dto.setAttemptsFinished(nullSafe(p.getAttemptsFinished()));
        dto.setAttemptsGraded(nullSafe(p.getAttemptsGraded()));
        dto.setTestsFinished(nullSafe(p.getTestsFinished()));
        dto.setTestsGraded(nullSafe(p.getTestsGraded()));
        dto.setCoursesStarted(nullSafe(p.getCoursesStarted()));
        dto.setCoursesCompleted(nullSafe(p.getCoursesCompleted()));

        List<StudentCourseProgressDto> courses = statisticsRepository.findStudentCourseProgress(current.getId())
                .stream().map(cp -> {
                    StudentCourseProgressDto c = new StudentCourseProgressDto();
                    c.setCourseId(cp.getCourseId());
                    c.setCourseName(cp.getCourseName());
                    c.setRequiredTests(nullSafe(cp.getRequiredTests()));
                    c.setCompletedTests(nullSafe(cp.getCompletedTests()));
                    c.setPercent(cp.getPercent());
                    c.setCompleted(cp.getCompleted());
                    return c;
                }).toList();

        dto.setCourses(courses);
        return dto;
    }

    public List<ClassTopicStatsDto> getClassTopicStatsForTeacher(Integer classId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_TEACHER);

        StudyClass sc = studyClassService.getEntityById(classId);
        if (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getTeacher().getId().equals(current.getId())) {
            throw new com.course.exception.ForbiddenOperationException("Teacher can view statistics only for own classes");
        }

        return statisticsRepository.findClassTopicStats(classId)
                .stream().map(p -> {
                    ClassTopicStatsDto dto = new ClassTopicStatsDto();
                    dto.setClassId(p.getClassId());
                    dto.setClassName(p.getClassName());
                    dto.setCourseId(p.getCourseId());
                    dto.setTopic(p.getTopic());
                    dto.setStudentsTotal(nullSafe(p.getStudentsTotal()));
                    dto.setStudentsWithActivity(nullSafe(p.getStudentsWithActivity()));
                    dto.setAvgPercent(p.getAvgPercent());
                    dto.setTestsAttempted(nullSafe(p.getTestsAttempted()));
                    return dto;
                }).toList();
    }

    public List<StudentTopicStatsDto> getStudentTopicStatsForTeacher(Integer studentId, Integer courseId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_TEACHER);

        if (courseId == null) {
            throw new com.course.exception.TestAttemptValidationException("courseId is required");
        }

        classStudentService.assertStudentInTeacherCourse(studentId, current.getId(), courseId,
                "Teacher can view statistics only for own students in the course");

        return statisticsRepository.findStudentTopicStats(studentId, courseId)
                .stream().map(p -> {
                    StudentTopicStatsDto dto = new StudentTopicStatsDto();
                    dto.setCourseId(p.getCourseId());
                    dto.setCourseName(p.getCourseName());
                    dto.setTopic(p.getTopic());
                    dto.setTestsAttempted(p.getTestsAttempted());
                    dto.setGradedTests(p.getGradedTests());
                    dto.setAttemptsCount(p.getAttemptsCount());
                    dto.setGradedAttemptsCount(p.getGradedAttemptsCount());
                    dto.setAvgBestPercent(p.getAvgBestPercent());
                    return dto;
                }).toList();
    }

    public List<CourseTopicStatsDto> getCourseTopicStatsForMethodist(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_METHODIST, ROLE_ADMIN);

        Course course = courseService.getEntityById(courseId);
        if (!isRole(current, ROLE_ADMIN)) {
            if (course.getCreatedBy() == null || course.getCreatedBy().getId() == null || !course.getCreatedBy().getId().equals(current.getId())) {
                throw new com.course.exception.ForbiddenOperationException("Methodist can view statistics only for own courses");
            }
        }

        return statisticsRepository.findCourseTopicStats(courseId)
                .stream().map(p -> {
                    CourseTopicStatsDto dto = new CourseTopicStatsDto();
                    dto.setCourseId(p.getCourseId());
                    dto.setCourseName(p.getCourseName());
                    dto.setTopic(p.getTopic());
                    dto.setStudentsTotal(nullSafe(p.getStudentsTotal()));
                    dto.setStudentsWithActivity(nullSafe(p.getStudentsWithActivity()));
                    dto.setAvgPercent(p.getAvgPercent());
                    dto.setTestsAttempted(nullSafe(p.getTestsAttempted()));
                    return dto;
                }).toList();
    }

    public List<TeacherClassTopicStatsDto> getCourseTeacherTopicStatsForMethodist(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_METHODIST, ROLE_ADMIN);

        Course course = courseService.getEntityById(courseId);
        if (!isRole(current, ROLE_ADMIN)) {
            if (course.getCreatedBy() == null || course.getCreatedBy().getId() == null || !course.getCreatedBy().getId().equals(current.getId())) {
                throw new com.course.exception.ForbiddenOperationException("Methodist can view statistics only for own courses");
            }
        }

        return statisticsRepository.findTeacherClassTopicStatsForCourse(courseId)
                .stream().map(p -> {
                    TeacherClassTopicStatsDto dto = new TeacherClassTopicStatsDto();
                    dto.setCourseId(p.getCourseId());
                    dto.setClassId(p.getClassId());
                    dto.setClassName(p.getClassName());
                    dto.setTeacherId(p.getTeacherId());
                    dto.setTeacherName(p.getTeacherName());
                    dto.setTopic(p.getTopic());
                    dto.setStudentsTotal(nullSafe(p.getStudentsTotal()));
                    dto.setStudentsWithActivity(nullSafe(p.getStudentsWithActivity()));
                    dto.setAvgPercent(p.getAvgPercent());
                    dto.setTestsAttempted(nullSafe(p.getTestsAttempted()));
                    return dto;
                }).toList();
    }

    // ---- small helpers ----

    private static Long nullSafe(Long v) {
        return v == null ? 0L : v;
    }

    private static String csv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        boolean needsQuotes = s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"");
        if (needsQuotes) {
            s = s.replace("\"", "\"\"");
            return '"' + s + '"';
        }
        return s;
    }

    private void assertAnyRole(User user, RoleName... roles) {
        for (RoleName r : roles) {
            if (isRole(user, r)) {
                return;
            }
        }
        throw new com.course.exception.ForbiddenOperationException("Access denied");
    }

    private boolean isRole(User user, RoleName role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && user.getRole().getRolename() == role;
    }
}
