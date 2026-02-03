package com.course.service;

import com.course.dto.*;
import com.course.entity.*;
import com.course.exception.*;
import com.course.repository.TestQuestionRepository;
import com.course.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_METHODIST = "METHODIST";

    private final TestRepository testRepository;
    private final TestQuestionRepository questionRepository;
    private final LessonService lessonService;
    private final UserService userService;
    private final AuthService authService;

    public TestDto create(Integer lessonId, CreateTestDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = lessonService.getEntityById(lessonId);
        // In this project, the creator of the lesson/course is the owner.
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can create tests");

        if (dto == null) {
            throw new TestValidationException("Test data is required");
        }

        if (testRepository.existsByLesson_Id(lessonId)) {
            throw new DuplicateResourceException("Test for lesson " + lessonId + " already exists");
        }

        String title = safeTrim(dto.getTitle());
        String description = safeTrim(dto.getDescription());
        String topic = safeTrim(dto.getTopic());
        LocalDateTime deadline = dto.getDeadline();

        if (!StringUtils.hasText(title)) {
            throw new TestValidationException("Test title must not be empty");
        }
        if (!StringUtils.hasText(topic)) {
            throw new TestValidationException("Test topic must not be empty");
        }
        if (deadline == null) {
            throw new TestValidationException("Test deadline is required");
        }

        Test test = new Test();
        test.setLesson(lesson);
        test.setCreatedBy(current);
        test.setTitle(title);
        test.setDescription(description);
        test.setTopic(topic);
        test.setDeadline(deadline);
        test.setStatus(TestStatus.DRAFT);
        test.setPublishedAt(null);

        return toDto(testRepository.save(test), true);
    }

    @Transactional(readOnly = true)
    public List<TestSummaryDto> listByLesson(Integer lessonId) {
        // ensures access to lesson (incl. student membership)
        lessonService.getEntityByIdForCurrentUser(lessonId);

        User current = authService.getCurrentUserEntity();
        boolean canSeeDrafts = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);

        List<Test> tests = canSeeDrafts
                ? testRepository.findAllByLesson_Id(lessonId)
                : testRepository.findAllByLesson_IdAndStatus(lessonId, TestStatus.READY);

        return tests.stream().map(this::toSummaryDto).toList();
    }

    /**
     * Returns TestDto for admin/methodist (includes correctOption),
     * otherwise returns TestPublicDto (without correctOption).
     */
    @Transactional(readOnly = true)
    public Object getById(Integer testId) {
        Test test = getEntityForCurrentUser(testId);
        User current = authService.getCurrentUserEntity();

        boolean canSeeCorrect = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);
        if (canSeeCorrect) {
            return toDto(test, true);
        }
        return toPublicDto(test);
    }

    public TestDto update(Integer testId, UpdateTestDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can edit this test");
        assertDraft(test, "Only DRAFT test can be updated");

        if (dto == null) {
            throw new TestValidationException("Test data is required");
        }

        String title = safeTrim(dto.getTitle());
        String description = safeTrim(dto.getDescription());
        String topic = safeTrim(dto.getTopic());
        LocalDateTime deadline = dto.getDeadline();

        if (!StringUtils.hasText(title)) {
            throw new TestValidationException("Test title must not be empty");
        }
        if (!StringUtils.hasText(topic)) {
            throw new TestValidationException("Test topic must not be empty");
        }
        if (deadline == null) {
            throw new TestValidationException("Test deadline is required");
        }

        test.setTitle(title);
        test.setDescription(description);
        test.setTopic(topic);
        test.setDeadline(deadline);
        return toDto(testRepository.save(test), true);
    }

    public void delete(Integer testId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can delete this test");
        assertDraft(test, "Only DRAFT test can be deleted");

        testRepository.delete(test);
    }

    /**
     * Publishes test (DRAFT -> READY). After this it becomes visible to all course participants.
     */
    public TestDto markReady(Integer testId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can publish this test");
        assertDraft(test, "Only DRAFT test can be published");

        int cnt = questionRepository.countByTest_Id(testId);
        if (cnt <= 0) {
            throw new TestValidationException("Cannot publish test without questions");
        }

        test.setStatus(TestStatus.READY);
        test.setPublishedAt(LocalDateTime.now());
        return toDto(testRepository.save(test), true);
    }

    // -------- Questions --------

    public TestQuestionDto createQuestion(Integer testId, CreateTestQuestionDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can edit questions");
        assertDraft(test, "Questions can be edited only while test is DRAFT");

        if (dto == null) {
            throw new TestQuestionValidationException("Question data is required");
        }

        Integer orderIndex = dto.getOrderIndex();
        if (orderIndex == null) {
            List<TestQuestion> existing = questionRepository.findAllByTest_IdOrderByOrderIndexAsc(testId);
            int last = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex();
            orderIndex = last + 1;
        }
        if (orderIndex < 1) {
            throw new TestQuestionValidationException("Question orderIndex must be >= 1");
        }
        if (questionRepository.existsByTest_IdAndOrderIndex(testId, orderIndex)) {
            throw new DuplicateResourceException("Question with orderIndex " + orderIndex + " already exists in this test");
        }

        TestQuestion q = new TestQuestion();
        q.setTest(test);
        q.setOrderIndex(orderIndex);
        q.setQuestionText(safeTrim(dto.getQuestionText()));
        q.setOption1(safeTrim(dto.getOption1()));
        q.setOption2(safeTrim(dto.getOption2()));
        q.setOption3(safeTrim(dto.getOption3()));
        q.setOption4(safeTrim(dto.getOption4()));
        q.setCorrectOption(dto.getCorrectOption());

        validateQuestionEntity(q);
        return toQuestionDto(questionRepository.save(q));
    }

    public TestQuestionDto updateQuestion(Integer testId, Integer questionId, UpdateTestQuestionDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can edit questions");
        assertDraft(test, "Questions can be edited only while test is DRAFT");

        TestQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new TestQuestionNotFoundException("Question with id " + questionId + " not found"));

        if (q.getTest() == null || q.getTest().getId() == null || !q.getTest().getId().equals(testId)) {
            throw new TestQuestionNotFoundException("Question with id " + questionId + " not found in test " + testId);
        }

        if (dto == null) {
            throw new TestQuestionValidationException("Question data is required");
        }

        Integer newOrder = dto.getOrderIndex();
        if (newOrder != null && newOrder < 1) {
            throw new TestQuestionValidationException("Question orderIndex must be >= 1");
        }
        if (newOrder != null && !newOrder.equals(q.getOrderIndex())
                && questionRepository.existsByTest_IdAndOrderIndex(testId, newOrder)) {
            throw new DuplicateResourceException("Question with orderIndex " + newOrder + " already exists in this test");
        }
        if (newOrder != null) {
            q.setOrderIndex(newOrder);
        }

        q.setQuestionText(safeTrim(dto.getQuestionText()));
        q.setOption1(safeTrim(dto.getOption1()));
        q.setOption2(safeTrim(dto.getOption2()));
        q.setOption3(safeTrim(dto.getOption3()));
        q.setOption4(safeTrim(dto.getOption4()));
        q.setCorrectOption(dto.getCorrectOption());

        validateQuestionEntity(q);
        return toQuestionDto(questionRepository.save(q));
    }

    public void deleteQuestion(Integer testId, Integer questionId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(testId);
        assertOwner(test.getCreatedBy(), current, "Only test creator can edit questions");
        assertDraft(test, "Questions can be edited only while test is DRAFT");

        TestQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new TestQuestionNotFoundException("Question with id " + questionId + " not found"));

        if (q.getTest() == null || q.getTest().getId() == null || !q.getTest().getId().equals(testId)) {
            throw new TestQuestionNotFoundException("Question with id " + questionId + " not found in test " + testId);
        }

        questionRepository.delete(q);
    }

    // -------- Entity access helpers --------

    @Transactional(readOnly = true)
    public Test getEntityById(Integer testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException("Test with id " + testId + " not found"));
    }

    @Transactional(readOnly = true)
    public Test getEntityForCurrentUser(Integer testId) {
        Test test = getEntityById(testId);

        if (test.getLesson() == null || test.getLesson().getId() == null) {
            throw new TestValidationException("Test lesson is missing");
        }
        // ensures lesson access (incl. student-in-course)
        lessonService.getEntityByIdForCurrentUser(test.getLesson().getId());

        User current = authService.getCurrentUserEntity();
        boolean canSeeDrafts = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);
        if (!canSeeDrafts && test.getStatus() != TestStatus.READY) {
            throw new TestAccessDeniedException("Test is not published yet");
        }
        return test;
    }

    // -------- Mappers --------

    private TestDto toDto(Test test, boolean includeCorrectAnswers) {
        TestDto dto = new TestDto();
        dto.setId(test.getId());

        if (test.getLesson() != null) {
            dto.setLessonId(test.getLesson().getId());
            if (test.getLesson().getCourse() != null) {
                dto.setCourseId(test.getLesson().getCourse().getId());
            }
        }

        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setTopic(test.getTopic());
        dto.setDeadline(test.getDeadline());
        dto.setStatus(test.getStatus() != null ? test.getStatus().name() : null);
        dto.setPublishedAt(test.getPublishedAt());

        if (test.getCreatedBy() != null) {
            dto.setCreatedById(test.getCreatedBy().getId());
            dto.setCreatedByName(test.getCreatedBy().getName());
        }

        dto.setCreatedAt(test.getCreatedAt());
        dto.setUpdatedAt(test.getUpdatedAt());

        List<TestQuestion> questions = questionRepository.findAllByTest_IdOrderByOrderIndexAsc(test.getId());
        dto.setQuestionCount(questions.size());
        dto.setQuestions(questions.stream().map(q -> {
            TestQuestionDto qdto = toQuestionDto(q);
            if (!includeCorrectAnswers) {
                qdto.setCorrectOption(null);
            }
            return qdto;
        }).toList());
        return dto;
    }

    private TestPublicDto toPublicDto(Test test) {
        TestPublicDto dto = new TestPublicDto();
        dto.setId(test.getId());

        if (test.getLesson() != null) {
            dto.setLessonId(test.getLesson().getId());
            if (test.getLesson().getCourse() != null) {
                dto.setCourseId(test.getLesson().getCourse().getId());
            }
        }

        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setTopic(test.getTopic());
        dto.setDeadline(test.getDeadline());
        dto.setPublishedAt(test.getPublishedAt());

        List<TestQuestion> questions = questionRepository.findAllByTest_IdOrderByOrderIndexAsc(test.getId());
        dto.setQuestionCount(questions.size());
        dto.setQuestions(questions.stream().map(this::toQuestionPublicDto).toList());
        return dto;
    }

    private TestSummaryDto toSummaryDto(Test test) {
        TestSummaryDto dto = new TestSummaryDto();
        dto.setId(test.getId());

        if (test.getLesson() != null) {
            dto.setLessonId(test.getLesson().getId());
            if (test.getLesson().getCourse() != null) {
                dto.setCourseId(test.getLesson().getCourse().getId());
            }
        }

        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setTopic(test.getTopic());
        dto.setDeadline(test.getDeadline());
        dto.setStatus(test.getStatus() != null ? test.getStatus().name() : null);
        dto.setPublishedAt(test.getPublishedAt());

        if (test.getCreatedBy() != null) {
            dto.setCreatedById(test.getCreatedBy().getId());
            dto.setCreatedByName(test.getCreatedBy().getName());
        }

        dto.setQuestionCount(questionRepository.countByTest_Id(test.getId()));
        dto.setCreatedAt(test.getCreatedAt());
        dto.setUpdatedAt(test.getUpdatedAt());
        return dto;
    }

    private TestQuestionDto toQuestionDto(TestQuestion q) {
        TestQuestionDto dto = new TestQuestionDto();
        dto.setId(q.getId());
        if (q.getTest() != null) {
            dto.setTestId(q.getTest().getId());
        }
        dto.setOrderIndex(q.getOrderIndex());
        dto.setQuestionText(q.getQuestionText());
        dto.setOption1(q.getOption1());
        dto.setOption2(q.getOption2());
        dto.setOption3(q.getOption3());
        dto.setOption4(q.getOption4());
        dto.setCorrectOption(q.getCorrectOption());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setUpdatedAt(q.getUpdatedAt());
        return dto;
    }

    private TestQuestionPublicDto toQuestionPublicDto(TestQuestion q) {
        TestQuestionPublicDto dto = new TestQuestionPublicDto();
        dto.setId(q.getId());
        if (q.getTest() != null) {
            dto.setTestId(q.getTest().getId());
        }
        dto.setOrderIndex(q.getOrderIndex());
        dto.setQuestionText(q.getQuestionText());
        dto.setOption1(q.getOption1());
        dto.setOption2(q.getOption2());
        dto.setOption3(q.getOption3());
        dto.setOption4(q.getOption4());
        return dto;
    }

    // -------- Validation / util --------

    private void validateQuestionEntity(TestQuestion q) {
        if (q == null) {
            throw new TestQuestionValidationException("Question data is required");
        }
        if (!StringUtils.hasText(q.getQuestionText())) {
            throw new TestQuestionValidationException("Question text must not be empty");
        }
        if (!StringUtils.hasText(q.getOption1()) || !StringUtils.hasText(q.getOption2())
                || !StringUtils.hasText(q.getOption3()) || !StringUtils.hasText(q.getOption4())) {
            throw new TestQuestionValidationException("All 4 options must be provided");
        }
        Integer co = q.getCorrectOption();
        if (co == null || co < 1 || co > 4) {
            throw new TestQuestionValidationException("correctOption must be between 1 and 4");
        }
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new TestAccessDeniedException(message);
        }
    }

    private void assertDraft(Test test, String message) {
        if (test == null || test.getStatus() == null || test.getStatus() != TestStatus.DRAFT) {
            throw new TestValidationException(message);
        }
    }

    private boolean isRole(User user, String role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role.equalsIgnoreCase(user.getRole().getRolename());
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
