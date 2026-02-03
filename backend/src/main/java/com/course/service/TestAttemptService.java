package com.course.service;

import com.course.dto.*;
import com.course.entity.*;
import com.course.exception.*;
import com.course.repository.TestAttemptAnswerRepository;
import com.course.repository.TestAttemptRepository;
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

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final TestService testService;
    private final LessonService lessonService;
    private final UserService userService;
    private final AuthService authService;

    private final TestAttemptRepository attemptRepository;
    private final TestAttemptAnswerRepository answerRepository;
    private final TestQuestionRepository questionRepository;

    /**
     * Starts a new attempt for a READY test.
     * If there is an existing IN_PROGRESS attempt for the student, returns it.
     */
    public TestAttemptDto startAttempt(Integer testId) {
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
            return toDto(existing.get(), true);
        }

        int previousCount = attemptRepository.countByTest_IdAndStudent_Id(testId, current.getId());

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setStudent(current);
        attempt.setAttemptNumber(previousCount + 1);
        attempt.setStatus(TestAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());

        return toDto(attemptRepository.save(attempt), true);
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
     * Lists all attempts for a test (teacher/methodist/admin).
     */
    @Transactional(readOnly = true)
    public List<TestAttemptSummaryDto> listAllAttempts(Integer testId) {
        User current = authService.getCurrentUserEntity();
        assertAnyRole(current, ROLE_TEACHER, ROLE_METHODIST, ROLE_ADMIN);

        Test test = testService.getEntityById(testId);
        // ensure the caller can view the lesson (students check; others allowed)
        if (test.getLesson() == null || test.getLesson().getId() == null) {
            throw new TestAttemptValidationException("Test lesson is missing");
        }
        lessonService.getEntityByIdForCurrentUser(test.getLesson().getId());

        return attemptRepository
                .findAllByTest_IdOrderByCreatedAtDesc(testId)
                .stream().map(this::toSummaryDto).toList();
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
            Integer selected = a.getSelectedOption();
            if (selected == null || selected < 1 || selected > 4) {
                throw new TestAttemptValidationException("selectedOption must be between 1 and 4");
            }
        }
        if (seen.size() != questions.size()) {
            throw new TestAttemptValidationException("All questions must be answered. Expected " + questions.size() + ", got " + seen.size());
        }

        // Remove any existing answers (defensive; should be empty for new attempts)
        attempt.getAnswers().clear();

        int correctCount = 0;
        for (SubmitTestAttemptAnswerDto a : dto.getAnswers()) {
            TestQuestion q = byId.get(a.getQuestionId());
            boolean isCorrect = q.getCorrectOption() != null && q.getCorrectOption().equals(a.getSelectedOption());
            if (isCorrect) {
                correctCount++;
            }

            TestAttemptAnswer ans = new TestAttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(q);
            ans.setSelectedOption(a.getSelectedOption());
            ans.setIsCorrect(isCorrect);
            attempt.getAnswers().add(ans);
        }

        attempt.setMaxScore(questions.size());
        attempt.setScore(correctCount);
        attempt.setStatus(TestAttemptStatus.GRADED);
        attempt.setSubmittedAt(LocalDateTime.now());

        TestAttempt saved = attemptRepository.save(attempt);
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
        dto.setCreatedAt(attempt.getCreatedAt());
        dto.setUpdatedAt(attempt.getUpdatedAt());

        if (includeAnswers) {
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
                adto.setIsCorrect(hideCorrectness ? null : a.getIsCorrect());
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
        dto.setCreatedAt(attempt.getCreatedAt());
        dto.setUpdatedAt(attempt.getUpdatedAt());
        return dto;
    }

    private Double calcPercent(Integer score, Integer max) {
        if (score == null || max == null || max <= 0) {
            return null;
        }
        return Math.round((score * 100.0 / max) * 100.0) / 100.0;
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
        if (attempt == null || attempt.getTest() == null || attempt.getTest().getLesson() == null) {
            throw new TestAttemptValidationException("Attempt data is invalid");
        }

        User current = authService.getCurrentUserEntity();

        if (isRole(current, ROLE_STUDENT)) {
            assertOwnerAttempt(attempt, current);
        }

        // ensures course/lesson access for everyone (students checked above; others pass)
        lessonService.getEntityByIdForCurrentUser(attempt.getTest().getLesson().getId());
    }

    private void assertAnyRole(User user, String... roles) {
        for (String r : roles) {
            if (isRole(user, r)) {
                return;
            }
        }
        throw new TestAttemptAccessDeniedException("Forbidden");
    }

    private boolean isRole(User user, String role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role.equalsIgnoreCase(user.getRole().getRolename());
    }
}
