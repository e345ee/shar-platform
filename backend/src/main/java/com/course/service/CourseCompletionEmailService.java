package com.course.service;

import com.course.entity.ActivityType;
import com.course.entity.Course;
import com.course.entity.RoleName;
import com.course.entity.Test;
import com.course.entity.TestAttempt;
import com.course.entity.TestAttemptStatus;
import com.course.entity.TestQuestion;
import com.course.entity.TestStatus;
import com.course.entity.User;
import com.course.exception.CourseNotClosedException;
import com.course.repository.TestAttemptRepository;
import com.course.repository.TestQuestionRepository;
import com.course.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCompletionEmailService {

    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final AuthService authService;
    private final UserService userService;
    private final CourseService courseService;
    private final ClassStudentService classStudentService;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final MailService mailService;
    private final CertificatePdfService certificatePdfService;

    
    @Transactional
    public void sendMyCompletionEmail(Integer courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }

        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);
        classStudentService.assertStudentInCourse(current.getId(), courseId, "Student is not enrolled in this course");

        if (!classStudentService.isCourseClosedForStudent(current.getId(), courseId)) {
            throw new CourseNotClosedException("Course is not closed for this student yet");
        }

        Course course = courseService.getEntityById(courseId);

        ScorePair pair = computeCourseScore(current.getId(), courseId);

        String courseName = course != null && course.getName() != null ? course.getName() : ("#" + courseId);


java.util.List<Integer> teacherIds = classStudentService.findDistinctTeacherIdsByStudentInCourse(current.getId(), courseId);
String teacherName = teacherIds == null || teacherIds.isEmpty()
        ? (course != null && course.getCreatedBy() != null ? course.getCreatedBy().getName() : "—")
        : teacherIds.stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .map(id -> {
                try {
                    return userService.getUserEntityById(id).getName();
                } catch (Exception ex) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .filter(s -> !s.isBlank())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("—");

byte[] pdf = certificatePdfService.generateCourseCertificate(course, teacherName, current, pair.earned, pair.max);

	String subject = "Сертификат: " + courseName;
	String text = "Поздравляем с завершением курса \"" + courseName + "\"!\n"
	        + "Ваш сертификат во вложении (PDF).";

String filename = ("certificate_" + courseName)
        .replaceAll("[^a-zA-Z0-9а-яА-Я._-]+", "_")
        .replaceAll("_+", "_");
if (!filename.toLowerCase().endsWith(".pdf")) {
    filename = filename + ".pdf";
}

mailService.sendToUserWithAttachment(
        current.getId(),
        subject,
        text,
        filename,
        pdf,
        "application/pdf"
);
    }

    private ScorePair computeCourseScore(Integer studentId, Integer courseId) {
        
        List<Test> tests = testRepository.findAllByCourse_IdAndStatusAndActivityTypeIn(
                courseId,
                TestStatus.READY,
                List.of(ActivityType.HOMEWORK_TEST, ActivityType.CONTROL_WORK)
        );

        int earned = 0;
        int max = 0;

        for (Test t : tests) {
            if (t == null || t.getId() == null) {
                continue;
            }
            int weight = (t.getWeightMultiplier() == null || t.getWeightMultiplier() < 1) ? 1 : t.getWeightMultiplier();

            int testMax = computeTestMaxPoints(t.getId());
            max += testMax * weight;

            TestAttempt attempt = testAttemptRepository
                    .findFirstByTest_IdAndStudent_IdAndStatusInOrderByAttemptNumberDesc(
                            t.getId(),
                            studentId,
                            List.of(TestAttemptStatus.SUBMITTED, TestAttemptStatus.GRADED)
                    )
                    .orElse(null);

            int score = attempt == null || attempt.getScore() == null ? 0 : attempt.getScore();
            earned += score * weight;
        }

        return new ScorePair(earned, max);
    }

    private int computeTestMaxPoints(Integer testId) {
        List<TestQuestion> questions = testQuestionRepository.findAllByTest_IdOrderByOrderIndexAsc(testId);
        int total = 0;
        for (TestQuestion q : questions) {
            if (q == null) {
                continue;
            }
            Integer pts = q.getPoints();
            total += (pts == null || pts < 1) ? 1 : pts;
        }
        return total;
    }

    private record ScorePair(int earned, int max) {}
}
