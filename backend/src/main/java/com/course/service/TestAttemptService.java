package com.course.service;

import com.course.dto.*;
import com.course.entity.*;
import com.course.exception.*;
import com.course.repository.TestAttemptAnswerRepository;
import com.course.repository.TestAttemptRepository;
import com.course.repository.PendingAttemptProjection;
import com.course.repository.TestQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TestAttemptService {

    /**
     * Result of starting an attempt.
     * <ul>
     *   <li>{@code created=true}  -> a new attempt was created (HTTP 201)</li>
     *   <li>{@code created=false} -> an existing IN_PROGRESS attempt was returned (HTTP 200)</li>
     * </ul>
     */
    public record StartAttemptResult(TestAttemptDto attempt, boolean created) {}

    private static final RoleName ROLE_ADMIN = RoleName.ADMIN;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final TestService testService;
    private final LessonService lessonService;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final ClassStudentService classStudentService;

    private final TestAttemptRepository attemptRepository;
    private final TestAttemptAnswerRepository answerRepository;
    private final TestQuestionRepository questionRepository;

    private final RemedialAssignmentService remedialAssignmentService;

    /**
     * Teacher grades OPEN questions in an attempt.
     * Supports partial grading (you can grade a subset of OPEN questions).
     * Attempt becomes GRADED automatically when all OPEN questions are graded.
     */
    public TestAttemptDto gradeOpenAttempt(Integer attemptId, GradeTestAttemptDto dto) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_TEACHER, ROLE_ADMIN);

        if (dto == null || dto.getGrades() == null || dto.getGrades().isEmpty()) {
            throw new TestAttemptValidationException("grades are required");
        }

        TestAttempt attempt = getEntityById(attemptId);

        if (attempt.getStatus() != TestAttemptStatus.SUBMITTED && attempt.getStatus() != TestAttemptStatus.GRADED) {
            throw new TestAttemptValidationException("Only SUBMITTED or GRADED attempts can be graded");
        }

        // Weekly activities are not bound to a lesson, so course must be taken from test.course
        if (attempt.getTest() == null || attempt.getTest().getCourse() == null) {
            throw new TestAttemptValidationException("Attempt data is invalid");
        }

        Integer courseId = attempt.getTest().getCourse().getId();
        Integer studentId = attempt.getStudent() != null ? attempt.getStudent().getId() : null;
        if (studentId == null || courseId == null) {
            throw new TestAttemptValidationException("Attempt data is invalid");
        }

        // teacher can grade only own students
        if (isRole(current, ROLE_TEACHER)) {
            classStudentService.assertStudentInTeacherCourse(studentId, current.getId(), courseId, "Teacher can grade only own students");
        }

        // methodists/admins can view course/lesson, but grading is only teacher/admin
        testService.getEntityForCurrentUser(attempt.getTest().getId());

        List<TestAttemptAnswer> answers = answerRepository.findAllByAttempt_IdOrderByIdAsc(attemptId);
        if (answers.isEmpty()) {
            throw new TestAttemptValidationException("Attempt has no answers");
        }

        // Build maps for OPEN answers
        Map<Integer, TestAttemptAnswer> openByQuestionId = new HashMap<>();
        for (TestAttemptAnswer a : answers) {
            TestQuestion q = a.getQuestion();
            if (q != null && q.getId() != null && q.getQuestionType() == TestQuestionType.OPEN) {
                openByQuestionId.put(q.getId(), a);
            }
        }
        if (openByQuestionId.isEmpty()) {
            throw new TestAttemptValidationException("Attempt does not contain OPEN questions");
        }

        // Validate: grades refer to existing OPEN questions (duplicates not allowed)
        Set<Integer> seen = new HashSet<>();
        for (GradeTestAttemptAnswerDto g : dto.getGrades()) {
            if (g == null || g.getQuestionId() == null) {
                throw new TestAttemptValidationException("Each grade must contain questionId");
            }
            if (!seen.add(g.getQuestionId())) {
                throw new TestAttemptValidationException("Duplicate grade for questionId=" + g.getQuestionId());
            }
            TestAttemptAnswer ans = openByQuestionId.get(g.getQuestionId());
            if (ans == null) {
                throw new TestAttemptValidationException("questionId=" + g.getQuestionId() + " is not an OPEN question in this attempt");
            }
            int max = (ans.getQuestion() != null && ans.getQuestion().getPoints() != null && ans.getQuestion().getPoints() > 0)
                    ? ans.getQuestion().getPoints()
                    : 1;
            int pa = g.getPointsAwarded() == null ? 0 : g.getPointsAwarded();
            if (pa < 0 || pa > max) {
                throw new TestAttemptValidationException("pointsAwarded for questionId=" + g.getQuestionId() + " must be between 0 and " + max);
            }
        }

        // Apply grades
        for (GradeTestAttemptAnswerDto g : dto.getGrades()) {
            TestAttemptAnswer ans = openByQuestionId.get(g.getQuestionId());
            int max = (ans.getQuestion() != null && ans.getQuestion().getPoints() != null && ans.getQuestion().getPoints() > 0)
                    ? ans.getQuestion().getPoints()
                    : 1;
            int pa = g.getPointsAwarded() == null ? 0 : g.getPointsAwarded();
            ans.setPointsAwarded(pa);
            // treat full points as correct (optional semantics)
            ans.setIsCorrect(pa == max);
            ans.setFeedback(safeTrim(g.getFeedback()));
            ans.setGradedAt(LocalDateTime.now());
        }

        // Recalculate totals
        int awardedTotal = 0;
        int maxTotal = 0;
        for (TestAttemptAnswer a : answers) {
            int qPoints = (a.getQuestion() != null && a.getQuestion().getPoints() != null && a.getQuestion().getPoints() > 0)
                    ? a.getQuestion().getPoints()
                    : 1;
            maxTotal += qPoints;
            awardedTotal += (a.getPointsAwarded() != null ? a.getPointsAwarded() : 0);
        }
        attempt.setMaxScore(maxTotal);
        attempt.setScore(awardedTotal);

        // Attempt becomes fully graded only when ALL OPEN answers have gradedAt
        boolean allOpenGraded = openByQuestionId.values().stream().allMatch(a -> a.getGradedAt() != null);
        attempt.setStatus(allOpenGraded ? TestAttemptStatus.GRADED : TestAttemptStatus.SUBMITTED);

        TestAttempt saved = attemptRepository.save(attempt);

        // If the attempt just became fully graded, consider assigning a remedial task by topic.
        if (saved.getStatus() == TestAttemptStatus.GRADED) {
            remedialAssignmentService.considerAssignAfterGrading(saved);
        }
        // Remedial attempts (should not have OPEN questions) are still safe to mark as completed here.
        remedialAssignmentService.markCompletedIfRemedial(saved);

        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<PendingTestAttemptDto> listPendingAttemptsForTeacher(Integer courseId, Integer testId, Integer classId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_TEACHER);

        List<PendingAttemptProjection> rows = attemptRepository.findPendingAttemptsForTeacher(
                current.getId(),
                courseId,
                testId,
                classId
        );

        return rows.stream().map(r -> {
            PendingTestAttemptDto dto = new PendingTestAttemptDto();
            dto.setAttemptId(r.getAttemptId());
            dto.setTestId(r.getTestId());
            dto.setLessonId(r.getLessonId());
            dto.setCourseId(r.getCourseId());
            dto.setClassId(r.getClassId());
            dto.setClassName(r.getClassName());
            dto.setStudentId(r.getStudentId());
            dto.setStudentName(r.getStudentName());
            dto.setUngradedOpenCount(r.getUngradedOpenCount());
            dto.setSubmittedAt(r.getSubmittedAt());
            return dto;
        }).toList();
    }

    /**
     * Starts a new attempt for a READY test.
     * If there is an existing IN_PROGRESS attempt for the student, returns it.
     */
    public StartAttemptResult startAttempt(Integer testId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        Test test = testService.getEntityForCurrentUser(testId); // ensures course membership + READY
        assertReady(test);
        assertBeforeDeadline(test);

        Optional<TestAttempt> existing = attemptRepository.findFirstByTest_IdAndStudent_IdAndStatusOrderByAttemptNumberDesc(
                testId,
                current.getId(),
                TestAttemptStatus.IN_PROGRESS
        );
        if (existing.isPresent()) {
            return new StartAttemptResult(toDto(existing.get(), true), false);
        }

        int previousCount = attemptRepository.countByTest_IdAndStudent_Id(testId, current.getId());

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setStudent(current);
        attempt.setAttemptNumber(previousCount + 1);
        attempt.setStatus(TestAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());

        return new StartAttemptResult(toDto(attemptRepository.save(attempt), true), true);
    }

    /**
     * Lists attempts of the current student for a test.
     */
    @Transactional(readOnly = true)
    public List<TestAttemptSummaryDto> listMyAttempts(Integer testId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        // ensures access & that test is visible
        Test test = testService.getEntityForCurrentUser(testId);
        assertReady(test);

        return attemptRepository
                .findAllByTest_IdAndStudent_IdOrderByAttemptNumberDesc(testId, current.getId())
                .stream().map(this::toSummaryDto).toList();
    }

/**
 * Student gets latest FINISHED attempt details for a test.
 * Returns 404 if there are no finished attempts.
 */
@Transactional(readOnly = true)
public TestAttemptDto getLatestCompletedAttemptForTest(Integer testId) {
    User current = authService.getCurrentUserEntity();
    userService.assertUserEntityHasRole(current, ROLE_STUDENT);

    // ensures student belongs to course and can see the test
    Test test = testService.getEntityForCurrentUser(testId);

    Optional<TestAttempt> latest = attemptRepository.findFirstByTest_IdAndStudent_IdAndStatusInOrderByAttemptNumberDesc(
            test.getId(),
            current.getId(),
            List.of(TestAttemptStatus.SUBMITTED, TestAttemptStatus.GRADED)
    );

    TestAttempt attempt = latest.orElseThrow(() ->
            new ResourceNotFoundException("No submitted/graded attempts for test " + testId)
    );

    return toDto(attempt, true);
}


    /**
     * Lists all attempts for a test (teacher/methodist/admin).
     */
    @Transactional(readOnly = true)
    public List<TestAttemptSummaryDto> listAllAttempts(Integer testId) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_TEACHER, ROLE_METHODIST, ROLE_ADMIN);

        Test test = testService.getEntityById(testId);
        // ensure the caller can view the activity (lesson gating for lesson-attached; course membership for weekly)
        testService.getEntityForCurrentUser(testId);

        Integer courseId = null;
        if (test.getLesson() != null && test.getLesson().getCourse() != null) {
            courseId = test.getLesson().getCourse().getId();
        } else if (test.getCourse() != null) {
            courseId = test.getCourse().getId();
        }

        // TEACHER: only attempts of own students (in teacher's classes within the course)
        if (isRole(current, ROLE_TEACHER)) {
            if (courseId == null) {
                throw new TestAttemptValidationException("Course is missing");
            }
            return attemptRepository
                    .findAllByTestIdForTeacher(testId, current.getId(), courseId)
                    .stream().map(this::toSummaryDto).toList();
        }

        // METHODIST/ADMIN: lesson access already restricts METHODIST to own courses; ADMIN can see all
        return attemptRepository
                .findAllByTest_IdOrderByCreatedAtDesc(testId)
                .stream().map(this::toSummaryDto).toList();
    }

    /**
     * Methodist/admin: list all test attempts for a given course.
     * Methodists see only their own courses.
     */
    @Transactional(readOnly = true)
    public List<CourseTestAttemptSummaryDto> listAttemptsForCourse(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_METHODIST, ROLE_ADMIN);

        // Ensure course exists and that the methodist owns it
        var course = courseService.getEntityById(courseId);
        if (isRole(current, ROLE_METHODIST)) {
            if (course.getCreatedBy() == null || course.getCreatedBy().getId() == null
                    || !course.getCreatedBy().getId().equals(current.getId())) {
                throw new TestAttemptAccessDeniedException("Methodist can access only own courses");
            }
        }

        return attemptRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)
                .stream().map(this::toCourseSummaryDto).toList();
    }

    /**
     * Returns an attempt. Student can read only their own attempts.
     * Teacher/methodist/admin can read attempts within the course/lesson.
     */
    @Transactional(readOnly = true)
    public TestAttemptDto getAttempt(Integer attemptId) {
        TestAttempt attempt = getEntityById(attemptId);
        assertCanViewAttempt(attempt);

        // show answers; hide correctness while IN_PROGRESS
        boolean includeAnswers = true;
        return toDto(attempt, includeAnswers);
    }

    /**
     * Submit attempt answers (student). Performs autograde.
     */
    public TestAttemptDto submit(Integer attemptId, SubmitTestAttemptDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        if (dto == null || dto.getAnswers() == null) {
            throw new TestAttemptValidationException("Answers are required");
        }

        TestAttempt attempt = getEntityById(attemptId);
        assertOwnerAttempt(attempt, current);

        if (attempt.getStatus() != TestAttemptStatus.IN_PROGRESS) {
            throw new TestAttemptValidationException("Only IN_PROGRESS attempt can be submitted");
        }

        Test test = attempt.getTest();
        if (test == null || test.getId() == null) {
            throw new TestAttemptValidationException("Attempt test is missing");
        }

        // also ensures the current student belongs to the course
        testService.getEntityForCurrentUser(test.getId());
        assertReady(test);
        assertBeforeDeadline(test);

        List<TestQuestion> questions = questionRepository.findAllByTest_IdOrderByOrderIndexAsc(test.getId());
        if (questions.isEmpty()) {
            throw new TestAttemptValidationException("Test has no questions");
        }

        Map<Integer, TestQuestion> byId = new HashMap<>();
        for (TestQuestion q : questions) {
            if (q.getId() != null) {
                byId.put(q.getId(), q);
            }
        }

        // Validate uniqueness and completeness
        Set<Integer> seen = new HashSet<>();
        for (SubmitTestAttemptAnswerDto a : dto.getAnswers()) {
            if (a == null || a.getQuestionId() == null) {
                throw new TestAttemptValidationException("Each answer must contain questionId");
            }
            if (!seen.add(a.getQuestionId())) {
                throw new TestAttemptValidationException("Duplicate answers for questionId=" + a.getQuestionId());
            }
            if (!byId.containsKey(a.getQuestionId())) {
                throw new TestAttemptValidationException("Question " + a.getQuestionId() + " does not belong to this test");
            }
            TestQuestion q = byId.get(a.getQuestionId());
            TestQuestionType type = q.getQuestionType() == null ? TestQuestionType.SINGLE_CHOICE : q.getQuestionType();
            if (type == TestQuestionType.SINGLE_CHOICE) {
                Integer selected = a.getSelectedOption();
                if (selected == null || selected < 1 || selected > 4) {
                    throw new TestAttemptValidationException("selectedOption must be between 1 and 4 for SINGLE_CHOICE");
                }
            } else if (type == TestQuestionType.TEXT || type == TestQuestionType.OPEN) {
                String text = a.getTextAnswer();
                if (text == null || text.trim().isEmpty()) {
                    throw new TestAttemptValidationException("textAnswer is required for " + type + " questions");
                }
            } else {
                throw new TestAttemptValidationException("Unsupported questionType for questionId=" + a.getQuestionId());
            }
        }
        if (seen.size() != questions.size()) {
            throw new TestAttemptValidationException("All questions must be answered. Expected " + questions.size() + ", got " + seen.size());
        }

        // Remove any existing answers (defensive; should be empty for new attempts)
        attempt.getAnswers().clear();

        int awardedTotal = 0;
        int maxTotal = 0;
        boolean hasOpenQuestions = false;
        for (SubmitTestAttemptAnswerDto a : dto.getAnswers()) {
            TestQuestion q = byId.get(a.getQuestionId());
            TestQuestionType type = q.getQuestionType() == null ? TestQuestionType.SINGLE_CHOICE : q.getQuestionType();
            if (type == TestQuestionType.OPEN) {
                hasOpenQuestions = true;
            }
            int qPoints = (q.getPoints() == null || q.getPoints() < 1) ? 1 : q.getPoints();
            maxTotal += qPoints;

            boolean isCorrect;
            Integer selectedOption = null;
            String textAnswer = null;
            if (type == TestQuestionType.SINGLE_CHOICE) {
                selectedOption = a.getSelectedOption();
                isCorrect = q.getCorrectOption() != null && q.getCorrectOption().equals(selectedOption);
            } else if (type == TestQuestionType.TEXT) {
                textAnswer = safeTrim(a.getTextAnswer());
                isCorrect = isTextAnswerCorrect(textAnswer, q.getCorrectTextAnswer());
            } else if (type == TestQuestionType.OPEN) {
                // OPEN questions are graded manually by the responsible teacher
                textAnswer = safeTrim(a.getTextAnswer());
                isCorrect = false;
            } else {
                throw new TestAttemptValidationException("Unsupported questionType for questionId=" + a.getQuestionId());
            }

            int pointsAwarded = (type == TestQuestionType.OPEN) ? 0 : (isCorrect ? qPoints : 0);
            awardedTotal += pointsAwarded;

            TestAttemptAnswer ans = new TestAttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(q);
            ans.setSelectedOption(selectedOption);
            ans.setTextAnswer(textAnswer);
            ans.setIsCorrect(isCorrect);
            ans.setPointsAwarded(pointsAwarded);
            ans.setFeedback(null);
            ans.setGradedAt(null);
            attempt.getAnswers().add(ans);
        }

        attempt.setMaxScore(maxTotal);
        attempt.setScore(awardedTotal);
        attempt.setStatus(hasOpenQuestions ? TestAttemptStatus.SUBMITTED : TestAttemptStatus.GRADED);
        attempt.setSubmittedAt(LocalDateTime.now());

        TestAttempt saved = attemptRepository.save(attempt);

        // Auto-graded attempt: if it became GRADED, consider remedial assignment by topic (< 50%).
        if (saved.getStatus() == TestAttemptStatus.GRADED) {
            remedialAssignmentService.considerAssignAfterGrading(saved);
        }
        // If the student completed a remedial activity, mark it as completed in the assignment table.
        remedialAssignmentService.markCompletedIfRemedial(saved);

        return toDto(saved, true);
    }

    // -------- Entity access helpers --------

    @Transactional(readOnly = true)
    public TestAttempt getEntityById(Integer attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new TestAttemptNotFoundException("Attempt with id " + attemptId + " not found"));
    }

    // -------- Mappers --------

    private TestAttemptDto toDto(TestAttempt attempt, boolean includeAnswers) {
        TestAttemptDto dto = new TestAttemptDto();
        dto.setId(attempt.getId());

        if (attempt.getTest() != null) {
            dto.setTestId(attempt.getTest().getId());
            if (attempt.getTest().getLesson() != null) {
                dto.setLessonId(attempt.getTest().getLesson().getId());
                if (attempt.getTest().getLesson().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getLesson().getCourse().getId());
                } else if (attempt.getTest().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getCourse().getId());
                } else if (attempt.getTest().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getCourse().getId());
                }
            }
        }

        if (attempt.getStudent() != null) {
            dto.setStudentId(attempt.getStudent().getId());
            dto.setStudentName(attempt.getStudent().getName());
        }

        dto.setAttemptNumber(attempt.getAttemptNumber());
        dto.setStatus(attempt.getStatus() != null ? attempt.getStatus().name() : null);
        dto.setStartedAt(attempt.getStartedAt());
        dto.setSubmittedAt(attempt.getSubmittedAt());
        dto.setScore(attempt.getScore());
        dto.setMaxScore(attempt.getMaxScore());
        dto.setPercent(calcPercent(attempt.getScore(), attempt.getMaxScore()));

        int w = (attempt.getTest() != null && attempt.getTest().getWeightMultiplier() != null) ? attempt.getTest().getWeightMultiplier() : 1;
        int ws = attempt.getScore() != null ? attempt.getScore() * w : 0;
        int wm = attempt.getMaxScore() != null ? attempt.getMaxScore() * w : 0;
        dto.setWeightedScore(ws);
        dto.setWeightedMaxScore(wm);
        dto.setWeightedPercent(calcPercent(ws, wm));
        dto.setCreatedAt(attempt.getCreatedAt());
        dto.setUpdatedAt(attempt.getUpdatedAt());

        if (includeAnswers) {
            User viewer = authService.getCurrentUserEntity();
            boolean viewerIsAdmin = isRole(viewer, ROLE_ADMIN);
            boolean viewerIsMethodist = isRole(viewer, ROLE_METHODIST);
            boolean viewerIsStudentOwner = isRole(viewer, ROLE_STUDENT)
                    && attempt.getStudent() != null
                    && attempt.getStudent().getId() != null
                    && attempt.getStudent().getId().equals(viewer.getId());
            // Must be effectively final because it's referenced inside a lambda below.
            boolean canCheckTeacherResponsibility = isRole(viewer, ROLE_TEACHER)
                    && attempt.getStudent() != null && attempt.getStudent().getId() != null
                    && attempt.getTest() != null && attempt.getTest().getLesson() != null
                    && attempt.getTest().getLesson().getCourse() != null
                    && attempt.getTest().getLesson().getCourse().getId() != null;
            final boolean viewerIsResponsibleTeacher = canCheckTeacherResponsibility
                    && classStudentService.existsStudentInTeacherCourse(
                    attempt.getStudent().getId(),
                    viewer.getId(),
                    attempt.getTest().getLesson().getCourse().getId()
            );

            List<TestAttemptAnswer> answers = answerRepository.findAllByAttempt_IdOrderByIdAsc(attempt.getId());
            boolean hideCorrectness = attempt.getStatus() == TestAttemptStatus.IN_PROGRESS;
            dto.setAnswers(answers.stream().map(a -> {
                TestAttemptAnswerDto adto = new TestAttemptAnswerDto();
                adto.setId(a.getId());
                adto.setAttemptId(attempt.getId());
                if (a.getQuestion() != null) {
                    adto.setQuestionId(a.getQuestion().getId());
                    adto.setQuestionOrderIndex(a.getQuestion().getOrderIndex());
                }
                adto.setSelectedOption(a.getSelectedOption());

                // Open-ended answers are visible only to:
                // - student who wrote them
                // - responsible teacher for the student's class
                // - admin
                // Methodists can see results, but not the open-ended answer content.
                TestQuestionType qType = (a.getQuestion() != null && a.getQuestion().getQuestionType() != null)
                        ? a.getQuestion().getQuestionType()
                        : TestQuestionType.SINGLE_CHOICE;
                boolean canSeeOpenAnswerText = viewerIsAdmin || viewerIsStudentOwner || viewerIsResponsibleTeacher;
                if (qType == TestQuestionType.OPEN && (!canSeeOpenAnswerText || viewerIsMethodist)) {
                    adto.setTextAnswer(null);
                } else {
                    adto.setTextAnswer(a.getTextAnswer());
                }

                if (qType == TestQuestionType.OPEN && (!canSeeOpenAnswerText || viewerIsMethodist)) {
                    adto.setFeedback(null);
                } else {
                    adto.setFeedback(a.getFeedback());
                }
                adto.setGradedAt(a.getGradedAt());
                adto.setIsCorrect(hideCorrectness ? null : a.getIsCorrect());
                adto.setPointsAwarded(hideCorrectness ? null : a.getPointsAwarded());
                adto.setCreatedAt(a.getCreatedAt());
                adto.setUpdatedAt(a.getUpdatedAt());
                return adto;
            }).toList());
        }

        return dto;
    }

    private TestAttemptSummaryDto toSummaryDto(TestAttempt attempt) {
        TestAttemptSummaryDto dto = new TestAttemptSummaryDto();
        dto.setId(attempt.getId());
        if (attempt.getTest() != null) {
            dto.setTestId(attempt.getTest().getId());
        }
        if (attempt.getStudent() != null) {
            dto.setStudentId(attempt.getStudent().getId());
            dto.setStudentName(attempt.getStudent().getName());
        }
        dto.setAttemptNumber(attempt.getAttemptNumber());
        dto.setStatus(attempt.getStatus() != null ? attempt.getStatus().name() : null);
        dto.setStartedAt(attempt.getStartedAt());
        dto.setSubmittedAt(attempt.getSubmittedAt());
        dto.setScore(attempt.getScore());
        dto.setMaxScore(attempt.getMaxScore());
        dto.setPercent(calcPercent(attempt.getScore(), attempt.getMaxScore()));

        int w = (attempt.getTest() != null && attempt.getTest().getWeightMultiplier() != null) ? attempt.getTest().getWeightMultiplier() : 1;
        int ws = attempt.getScore() != null ? attempt.getScore() * w : 0;
        int wm = attempt.getMaxScore() != null ? attempt.getMaxScore() * w : 0;
        dto.setWeightedScore(ws);
        dto.setWeightedMaxScore(wm);
        dto.setWeightedPercent(calcPercent(ws, wm));
        dto.setCreatedAt(attempt.getCreatedAt());
        dto.setUpdatedAt(attempt.getUpdatedAt());
        return dto;
    }

    private CourseTestAttemptSummaryDto toCourseSummaryDto(TestAttempt attempt) {
        CourseTestAttemptSummaryDto dto = new CourseTestAttemptSummaryDto();
        dto.setAttemptId(attempt.getId());
        if (attempt.getTest() != null) {
            dto.setTestId(attempt.getTest().getId());
            if (attempt.getTest().getLesson() != null) {
                dto.setLessonId(attempt.getTest().getLesson().getId());
                if (attempt.getTest().getLesson().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getLesson().getCourse().getId());
                } else if (attempt.getTest().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getCourse().getId());
                } else if (attempt.getTest().getCourse() != null) {
                    dto.setCourseId(attempt.getTest().getCourse().getId());
                }
            }
        }
        if (attempt.getStudent() != null) {
            dto.setStudentId(attempt.getStudent().getId());
            dto.setStudentName(attempt.getStudent().getName());
        }
        dto.setAttemptNumber(attempt.getAttemptNumber());
        dto.setStatus(attempt.getStatus() != null ? attempt.getStatus().name() : null);
        dto.setScore(attempt.getScore());
        dto.setMaxScore(attempt.getMaxScore());
        dto.setPercent(calcPercent(attempt.getScore(), attempt.getMaxScore()));

        int w = (attempt.getTest() != null && attempt.getTest().getWeightMultiplier() != null) ? attempt.getTest().getWeightMultiplier() : 1;
        int ws = attempt.getScore() != null ? attempt.getScore() * w : 0;
        int wm = attempt.getMaxScore() != null ? attempt.getMaxScore() * w : 0;
        dto.setWeightedScore(ws);
        dto.setWeightedMaxScore(wm);
        dto.setWeightedPercent(calcPercent(ws, wm));
        dto.setSubmittedAt(attempt.getSubmittedAt());
        dto.setCreatedAt(attempt.getCreatedAt());
        return dto;
    }

    private Double calcPercent(Integer score, Integer max) {
        if (score == null || max == null || max <= 0) {
            return null;
        }
        return Math.round((score * 100.0 / max) * 100.0) / 100.0;
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * TEXT matching rule: trim + case-insensitive compare.
     */
    private boolean isTextAnswerCorrect(String studentAnswer, String correctAnswer) {
        if (correctAnswer == null) {
            return false;
        }
        String c = correctAnswer.trim();
        if (c.isEmpty()) {
            return false;
        }
        String s = studentAnswer == null ? "" : studentAnswer.trim();
        return c.equalsIgnoreCase(s);
    }

    // -------- Guards --------

    private void assertReady(Test test) {
        if (test == null || test.getStatus() != TestStatus.READY) {
            throw new TestAttemptValidationException("Test is not published");
        }
    }

    private void assertBeforeDeadline(Test test) {
        if (test == null || test.getDeadline() == null) {
            return;
        }
        if (LocalDateTime.now().isAfter(test.getDeadline())) {
            throw new TestAttemptValidationException("Test deadline has passed");
        }
    }

    private void assertOwnerAttempt(TestAttempt attempt, User current) {
        if (attempt == null || attempt.getStudent() == null || attempt.getStudent().getId() == null
                || current == null || current.getId() == null
                || !attempt.getStudent().getId().equals(current.getId())) {
            throw new TestAttemptAccessDeniedException("You can only access your own attempts");
        }
    }

    private void assertCanViewAttempt(TestAttempt attempt) {
        // Weekly activities are not bound to a lesson, so don't require test.lesson here.
        if (attempt == null || attempt.getTest() == null || attempt.getTest().getCourse() == null) {
            throw new TestAttemptValidationException("Attempt data is invalid");
        }

        User current = authService.getCurrentUserEntity();

        if (isRole(current, ROLE_STUDENT)) {
            assertOwnerAttempt(attempt, current);
        }

        if (isRole(current, ROLE_TEACHER)) {
            Integer studentId = attempt.getStudent() != null ? attempt.getStudent().getId() : null;
            Integer courseId = attempt.getTest().getCourse() != null ? attempt.getTest().getCourse().getId() : null;
            if (studentId == null || courseId == null) {
                throw new TestAttemptValidationException("Attempt data is invalid");
            }
            classStudentService.assertStudentInTeacherCourse(
                    studentId,
                    current.getId(),
                    courseId,
                    "Teacher can access only own students"
            );
        }

        // ensures course/lesson access for everyone (students checked above; others pass)
        testService.getEntityForCurrentUser(attempt.getTest().getId());
    }

    private void assertAnyRole(User user, RoleName... roles) {
        for (RoleName r : roles) {
            if (isRole(user, r)) {
                return;
            }
        }
        throw new TestAttemptAccessDeniedException("Forbidden");
    }

    private boolean isRole(User user, RoleName role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role == user.getRole().getRolename();
    }
}
