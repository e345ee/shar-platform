package com.course.service;

import com.course.dto.activity.ActivityCreateRequest;
import com.course.dto.activity.ActivityQuestionResponse;
import com.course.dto.activity.ActivityQuestionUpsertRequest;
import com.course.dto.activity.ActivityResponse;
import com.course.dto.activity.ActivityUpsertRequest;
import com.course.dto.activity.WeeklyActivityAssignRequest;
import com.course.entity.*;
import com.course.exception.*;
import com.course.repository.TestQuestionRepository;
import com.course.repository.TestRepository;
import com.course.repository.StudentRemedialAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private static final RoleName ROLE_ADMIN = RoleName.ADMIN;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final TestRepository testRepository;
    private final TestQuestionRepository questionRepository;
    private final LessonService lessonService;
    private final CourseService courseService;
    private final ClassStudentService classStudentService;
    private final ClassOpenedTestService classOpenedTestService;
    private final StudyClassService studyClassService;
    private final UserService userService;
    private final AuthService authService;
    private final StudentRemedialAssignmentRepository studentRemedialAssignmentRepository;
    private final NotificationService notificationService;

    public ActivityResponse create(Integer lessonId, ActivityUpsertRequest dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = lessonService.getEntityById(lessonId);
        
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can create tests");

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

        Test test = new Test();
        test.setLesson(lesson);
        test.setCourse(lesson.getCourse());
        test.setActivityType(ActivityType.HOMEWORK_TEST);
        test.setWeightMultiplier(1);
        test.setCreatedBy(current);
        test.setTitle(title);
        test.setDescription(description);
        test.setTopic(topic);
        test.setDeadline(deadline);
        test.setStatus(TestStatus.DRAFT);
        test.setPublishedAt(null);

        return toDto(testRepository.save(test), true);
    }

    
    public ActivityResponse createActivity(Integer courseId, ActivityCreateRequest dto) {
        User current = authService.getCurrentUserEntity();
        
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = courseService.getEntityById(courseId);
        assertOwner(course.getCreatedBy(), current, "Only course creator can create activities");

        if (dto == null) {
            throw new TestValidationException("Activity data is required");
        }

        ActivityType type;
        try {
            type = ActivityType.valueOf(safeTrim(dto.getActivityType()));
        } catch (Exception e) {
            throw new TestValidationException("Unknown activityType");
        }

        Integer lessonId = dto.getLessonId();
        Lesson lesson = null;
        if (type == ActivityType.HOMEWORK_TEST) {
            if (lessonId == null) {
                throw new TestValidationException("lessonId is required for HOMEWORK_TEST");
            }
            lesson = lessonService.getEntityById(lessonId);
            if (!lesson.getCourse().getId().equals(courseId)) {
                throw new TestValidationException("Lesson does not belong to course");
            }
            
        } else if (type == ActivityType.CONTROL_WORK) {
            if (lessonId != null) {
                lesson = lessonService.getEntityById(lessonId);
                if (!lesson.getCourse().getId().equals(courseId)) {
                    throw new TestValidationException("Lesson does not belong to course");
                }
                if (testRepository.existsByLesson_IdAndActivityType(lessonId, ActivityType.CONTROL_WORK)) {
                    throw new DuplicateResourceException("Control work for lesson " + lessonId + " already exists");
                }
            }
        } else if (type == ActivityType.WEEKLY_STAR) {
            if (lessonId != null) {
                throw new TestValidationException("lessonId must be null for WEEKLY_STAR");
            }
        } else if (type == ActivityType.REMEDIAL_TASK) {
            if (lessonId != null) {
                throw new TestValidationException("lessonId must be null for REMEDIAL_TASK");
            }
        }

        String title = safeTrim(dto.getTitle());
        String description = safeTrim(dto.getDescription());
        String topic = safeTrim(dto.getTopic());
        LocalDateTime deadline = dto.getDeadline();

        if (!StringUtils.hasText(title)) {
            throw new TestValidationException("Activity title must not be empty");
        }
        if (!StringUtils.hasText(topic)) {
            throw new TestValidationException("Activity topic must not be empty");
        }
        if (deadline == null) {
            throw new TestValidationException("Activity deadline is required");
        }

        int defaultWeight = (type == ActivityType.HOMEWORK_TEST || type == ActivityType.REMEDIAL_TASK) ? 1 : 2;
        Integer weight = dto.getWeightMultiplier() != null ? dto.getWeightMultiplier() : defaultWeight;
        if (weight == null || weight < 1 || weight > 100) {
            throw new TestValidationException("weightMultiplier must be between 1 and 100");
        }

        Integer timeLimitSeconds = null;
        if (type == ActivityType.CONTROL_WORK) {
            Integer tls = dto.getTimeLimitSeconds();
            timeLimitSeconds = tls != null ? tls : 3600;
            if (timeLimitSeconds < 1 || timeLimitSeconds > 86400) {
                throw new TestValidationException("timeLimitSeconds must be between 1 and 86400");
            }
        } else if (dto.getTimeLimitSeconds() != null) {
            throw new TestValidationException("timeLimitSeconds is supported only for CONTROL_WORK");
        }

        Test test = new Test();
        test.setCourse(course);
        test.setLesson(lesson);
        test.setCreatedBy(current);
        test.setTitle(title);
        test.setDescription(description);
        test.setTopic(topic);
        test.setDeadline(deadline);
        test.setActivityType(type);
        test.setWeightMultiplier(weight);
        test.setStatus(TestStatus.DRAFT);
        test.setPublishedAt(null);
        test.setAssignedWeekStart(null);
        test.setTimeLimitSeconds(timeLimitSeconds);

        return toDto(testRepository.save(test), true);
    }

    
    public ActivityResponse assignWeeklyActivity(Integer activityId, WeeklyActivityAssignRequest dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Test test = getEntityById(activityId);
        if (test.getActivityType() != ActivityType.WEEKLY_STAR && test.getActivityType() != ActivityType.REMEDIAL_TASK) {
            throw new TestValidationException("Only WEEKLY_STAR or REMEDIAL_TASK can be assigned to a week");
        }
        assertOwner(test.getCourse().getCreatedBy(), current, "Only course creator can assign weekly activity");

        if (dto == null || dto.getWeekStart() == null) {
            throw new TestValidationException("weekStart is required");
        }
        LocalDate weekStart = dto.getWeekStart();
        
        if (weekStart.getDayOfWeek().getValue() != 1) {
            throw new TestValidationException("weekStart must be a Monday date");
        }

        if (test.getStatus() != TestStatus.READY) {
            throw new TestValidationException("Weekly activity must be READY before assigning");
        }

        test.setAssignedWeekStart(weekStart);
        Test saved = testRepository.save(test);

        
        if (saved.getCourse() != null && saved.getCourse().getId() != null) {
            Integer courseId = saved.getCourse().getId();
            java.util.List<Integer> studentIds = classStudentService.findDistinctStudentIdsByCourseId(courseId);
            for (Integer sid : studentIds) {
                if (sid == null) continue;
                try {
                    User student = userService.getUserEntityById(sid);
                    notificationService.create(student,
                            NotificationType.WEEKLY_ASSIGNMENT_AVAILABLE,
                            "Доступно новое недельное задание",
                            "В курсе '" + saved.getCourse().getName() + "' доступно недельное задание: " + saved.getTitle(),
                            courseId,
                            null,
                            saved.getId(),
                            null,
                            null);
                } catch (Exception ignored) {
                    
                }
            }
        }

        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listWeeklyActivitiesForCourse(Integer courseId) {
        
        User current = authService.getCurrentUserEntity();
        Course course = courseService.getEntityById(courseId);

        boolean canSeeDrafts = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);
        if (isRole(current, ROLE_METHODIST)) {
            assertOwner(course.getCreatedBy(), current, "Only course creator can view this course activities");
        }
        if (isRole(current, ROLE_STUDENT)) {
            classStudentService.assertStudentInCourse(current.getId(), courseId, "Student is not enrolled in this course");
        }
        if (isRole(current, ROLE_TEACHER)) {
            studyClassService.assertTeacherCanManageCourse(courseId, current);
        }

        List<Test> tests = canSeeDrafts
                ? testRepository.findAllByCourse_IdAndActivityTypeAndStatusAndAssignedWeekStartNotNullOrderByAssignedWeekStartDesc(courseId, ActivityType.WEEKLY_STAR, TestStatus.READY)
                : testRepository.findAllByCourse_IdAndActivityTypeAndStatusAndAssignedWeekStartNotNullOrderByAssignedWeekStartDesc(courseId, ActivityType.WEEKLY_STAR, TestStatus.READY);

        return tests.stream().map(this::toSummaryDto).toList();
    }


    @Transactional(readOnly = true)
    public List<ActivityResponse> listRemedialActivitiesForCourse(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        Course course = courseService.getEntityById(courseId);

        if (isRole(current, ROLE_METHODIST)) {
            assertOwner(course.getCreatedBy(), current, "Only course creator can view this course activities");
        }
        if (isRole(current, ROLE_TEACHER)) {
            studyClassService.assertTeacherCanManageCourse(courseId, current);
        }

        List<Test> tests = testRepository.findAllByCourse_IdAndStatusAndActivityTypeIn(
                courseId,
                TestStatus.READY,
                List.of(ActivityType.REMEDIAL_TASK)
        );
        return tests.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listByLesson(Integer lessonId) {
        
        lessonService.getEntityByIdForCurrentUser(lessonId);

        User current = authService.getCurrentUserEntity();
        boolean canSeeDrafts = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);

        List<Test> tests = canSeeDrafts
                ? testRepository.findAllByLesson_Id(lessonId)
                : testRepository.findAllByLesson_IdAndStatus(lessonId, TestStatus.READY);

        
        
        if (isRole(current, ROLE_STUDENT) && !tests.isEmpty()) {
            List<Integer> opened = classOpenedTestService.findOpenedTestIdsForStudentInLesson(current.getId(), lessonId);
            if (opened.isEmpty()) {
                return List.of();
            }
            tests = tests.stream().filter(t -> t.getId() != null && opened.contains(t.getId())).toList();
        }

        return tests.stream().map(this::toSummaryDto).toList();
    }

    
    @Transactional(readOnly = true)
    public ActivityResponse getById(Integer testId) {
        Test test = getEntityForCurrentUser(testId);
        User current = authService.getCurrentUserEntity();

        boolean canSeeCorrect = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);
        return toDto(test, canSeeCorrect);
    }

    public ActivityResponse update(Integer testId, ActivityUpsertRequest dto) {
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

        if (dto.getTimeLimitSeconds() != null) {
            if (test.getActivityType() != ActivityType.CONTROL_WORK) {
                throw new TestValidationException("timeLimitSeconds is supported only for CONTROL_WORK");
            }
            Integer tls = dto.getTimeLimitSeconds();
            if (tls < 1 || tls > 86400) {
                throw new TestValidationException("timeLimitSeconds must be between 1 and 86400");
            }
            test.setTimeLimitSeconds(tls);
        }
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

    
    public ActivityResponse markReady(Integer testId) {
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

    

    public ActivityQuestionResponse createQuestion(Integer testId, ActivityQuestionUpsertRequest dto) {
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

        TestQuestionType type = parseQuestionType(dto.getQuestionType(), TestQuestionType.SINGLE_CHOICE);
        if (test.getActivityType() == ActivityType.REMEDIAL_TASK && type == TestQuestionType.OPEN) {
            throw new TestQuestionValidationException("REMEDIAL_TASK cannot contain OPEN questions");
        }
        q.setQuestionType(type);
        q.setPoints(dto.getPoints() == null ? 1 : dto.getPoints());

        if (type == TestQuestionType.SINGLE_CHOICE) {
            q.setOption1(safeTrim(dto.getOption1()));
            q.setOption2(safeTrim(dto.getOption2()));
            q.setOption3(safeTrim(dto.getOption3()));
            q.setOption4(safeTrim(dto.getOption4()));
            q.setCorrectOption(dto.getCorrectOption());
            q.setCorrectTextAnswer(null);
        } else if (type == TestQuestionType.TEXT) {
            
            q.setOption1(null);
            q.setOption2(null);
            q.setOption3(null);
            q.setOption4(null);
            q.setCorrectOption(null);
            q.setCorrectTextAnswer(safeTrim(dto.getCorrectTextAnswer()));
        } else if (type == TestQuestionType.OPEN) {
            
            q.setOption1(null);
            q.setOption2(null);
            q.setOption3(null);
            q.setOption4(null);
            q.setCorrectOption(null);
            q.setCorrectTextAnswer(null);
        }

        validateQuestionEntity(q);
        return (ActivityQuestionResponse) toQuestionDto(questionRepository.save(q));
    }

    public ActivityQuestionResponse updateQuestion(Integer testId, Integer questionId, ActivityQuestionUpsertRequest dto) {
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

        if (dto.getQuestionType() != null) {
            TestQuestionType newType = parseQuestionType(dto.getQuestionType(), q.getQuestionType());
            if (test.getActivityType() == ActivityType.REMEDIAL_TASK && newType == TestQuestionType.OPEN) {
                throw new TestQuestionValidationException("REMEDIAL_TASK cannot contain OPEN questions");
            }
            q.setQuestionType(newType);
        }
        if (dto.getPoints() != null) {
            q.setPoints(dto.getPoints());
        }

        if (q.getQuestionType() == TestQuestionType.SINGLE_CHOICE) {
            q.setOption1(safeTrim(dto.getOption1()));
            q.setOption2(safeTrim(dto.getOption2()));
            q.setOption3(safeTrim(dto.getOption3()));
            q.setOption4(safeTrim(dto.getOption4()));
            q.setCorrectOption(dto.getCorrectOption());
            q.setCorrectTextAnswer(null);
        } else if (q.getQuestionType() == TestQuestionType.TEXT) {
            q.setOption1(null);
            q.setOption2(null);
            q.setOption3(null);
            q.setOption4(null);
            q.setCorrectOption(null);
            q.setCorrectTextAnswer(safeTrim(dto.getCorrectTextAnswer()));
        } else if (q.getQuestionType() == TestQuestionType.OPEN) {
            q.setOption1(null);
            q.setOption2(null);
            q.setOption3(null);
            q.setOption4(null);
            q.setCorrectOption(null);
            q.setCorrectTextAnswer(null);
        }

        validateQuestionEntity(q);
        return (ActivityQuestionResponse) toQuestionDto(questionRepository.save(q));
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

    

    @Transactional(readOnly = true)
    public Test getEntityById(Integer testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new TestNotFoundException("Test with id " + testId + " not found"));
    }

    @Transactional(readOnly = true)
    public Test getEntityForCurrentUser(Integer testId) {
        Test test = getEntityById(testId);

        if (test.getLesson() != null && test.getLesson().getId() != null) {
            
            lessonService.getEntityByIdForCurrentUser(test.getLesson().getId());

            
            User currentUser = authService.getCurrentUserEntity();
            if (isRole(currentUser, ROLE_STUDENT)) {
                classOpenedTestService.assertTestOpenedForStudent(
                        currentUser.getId(),
                        test.getId(),
                        "Test is not opened for your class yet"
                );
            }
        } else {
            
            if (test.getCourse() == null || test.getCourse().getId() == null) {
                throw new TestValidationException("Test course is missing");
            }
            User currentUser = authService.getCurrentUserEntity();
            Integer courseId = test.getCourse().getId();

            if (isRole(currentUser, ROLE_METHODIST)) {
                assertOwner(test.getCourse().getCreatedBy(), currentUser, "Methodist can access only own courses");
            } else if (isRole(currentUser, ROLE_TEACHER)) {
                studyClassService.assertTeacherCanManageCourse(courseId, currentUser);
            } else if (isRole(currentUser, ROLE_STUDENT)) {
                classStudentService.assertStudentInCourse(currentUser.getId(), courseId, "Student is not enrolled in this course");

                
                if (test.getActivityType() == ActivityType.REMEDIAL_TASK) {
                    boolean assigned = studentRemedialAssignmentRepository.existsByStudent_IdAndTest_Id(currentUser.getId(), test.getId());
                    if (!assigned) {
                        throw new ForbiddenOperationException("Remedial activity is not assigned to you");
                    }
                }
            }
        }

        User current = authService.getCurrentUserEntity();
        boolean canSeeDrafts = isRole(current, ROLE_METHODIST) || isRole(current, ROLE_ADMIN);
        if (!canSeeDrafts && test.getStatus() != TestStatus.READY) {
            throw new TestAccessDeniedException("Test is not published yet");
        }
        return test;
    }

    

    private ActivityResponse toDto(Test test, boolean includeCorrectAnswers) {
        ActivityResponse dto = new ActivityResponse();
        dto.setId(test.getId());

        dto.setActivityType(test.getActivityType() != null ? test.getActivityType().name() : null);
        dto.setWeightMultiplier(test.getWeightMultiplier());
        dto.setAssignedWeekStart(test.getAssignedWeekStart());
        dto.setTimeLimitSeconds(test.getTimeLimitSeconds());

        if (test.getLesson() != null) {
            dto.setLessonId(test.getLesson().getId());
            if (test.getLesson().getCourse() != null) {
                dto.setCourseId(test.getLesson().getCourse().getId());
            }
        }
        if (dto.getCourseId() == null && test.getCourse() != null) {
            dto.setCourseId(test.getCourse().getId());
        }
        if (dto.getCourseId() == null && test.getCourse() != null) {
            dto.setCourseId(test.getCourse().getId());
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
        if (!includeCorrectAnswers) {
            
            dto.setStatus(null);
            dto.setCreatedById(null);
            dto.setCreatedByName(null);
            dto.setCreatedAt(null);
            dto.setUpdatedAt(null);
        }


        List<TestQuestion> questions = questionRepository.findAllByTest_IdOrderByOrderIndexAsc(test.getId());
        dto.setQuestionCount(questions.size());
        dto.setQuestions(questions.stream().map(q -> {
            ActivityQuestionResponse qdto = toQuestionDto(q);
            if (!includeCorrectAnswers) {
                qdto.setCorrectOption(null);
                qdto.setCorrectTextAnswer(null);
            }
            return qdto;
        }).toList());
        return dto;
    }


    
    public ActivityResponse toSummaryDto(Test test) {
        ActivityResponse dto = new ActivityResponse();
        dto.setId(test.getId());

        dto.setActivityType(test.getActivityType() != null ? test.getActivityType().name() : null);
        dto.setWeightMultiplier(test.getWeightMultiplier());
        dto.setAssignedWeekStart(test.getAssignedWeekStart());
        dto.setTimeLimitSeconds(test.getTimeLimitSeconds());

        if (test.getLesson() != null) {
            dto.setLessonId(test.getLesson().getId());
            if (test.getLesson().getCourse() != null) {
                dto.setCourseId(test.getLesson().getCourse().getId());
            }
        }
        if (dto.getCourseId() == null && test.getCourse() != null) {
            dto.setCourseId(test.getCourse().getId());
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

    private ActivityQuestionResponse toQuestionDto(TestQuestion q) {
        ActivityQuestionResponse dto = new ActivityQuestionResponse();
        dto.setId(q.getId());
        if (q.getTest() != null) {
            dto.setTestId(q.getTest().getId());
        }
        dto.setOrderIndex(q.getOrderIndex());
        dto.setQuestionText(q.getQuestionText());
        dto.setQuestionType(q.getQuestionType() != null ? q.getQuestionType().name() : null);
        dto.setPoints(q.getPoints());
        dto.setOption1(q.getOption1());
        dto.setOption2(q.getOption2());
        dto.setOption3(q.getOption3());
        dto.setOption4(q.getOption4());
        dto.setCorrectOption(q.getCorrectOption());
        dto.setCorrectTextAnswer(q.getCorrectTextAnswer());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setUpdatedAt(q.getUpdatedAt());
        return dto;
    }

    

    private void validateQuestionEntity(TestQuestion q) {
        if (q == null) {
            throw new TestQuestionValidationException("Question data is required");
        }
        if (!StringUtils.hasText(q.getQuestionText())) {
            throw new TestQuestionValidationException("Question text must not be empty");
        }
        if (q.getPoints() == null || q.getPoints() < 1) {
            throw new TestQuestionValidationException("points must be >= 1");
        }

        TestQuestionType type = q.getQuestionType() == null ? TestQuestionType.SINGLE_CHOICE : q.getQuestionType();
        if (type == TestQuestionType.SINGLE_CHOICE) {
            if (!StringUtils.hasText(q.getOption1()) || !StringUtils.hasText(q.getOption2())
                    || !StringUtils.hasText(q.getOption3()) || !StringUtils.hasText(q.getOption4())) {
                throw new TestQuestionValidationException("All 4 options must be provided for SINGLE_CHOICE");
            }
            Integer co = q.getCorrectOption();
            if (co == null || co < 1 || co > 4) {
                throw new TestQuestionValidationException("correctOption must be between 1 and 4");
            }
        } else if (type == TestQuestionType.TEXT) {
            if (!StringUtils.hasText(q.getCorrectTextAnswer())) {
                throw new TestQuestionValidationException("correctTextAnswer must be provided for TEXT");
            }
        } else if (type == TestQuestionType.OPEN) {
            
            
            if (StringUtils.hasText(q.getOption1()) || StringUtils.hasText(q.getOption2())
                    || StringUtils.hasText(q.getOption3()) || StringUtils.hasText(q.getOption4())) {
                throw new TestQuestionValidationException("Options must not be provided for OPEN");
            }
            if (q.getCorrectOption() != null) {
                throw new TestQuestionValidationException("correctOption must be null for OPEN");
            }
            if (StringUtils.hasText(q.getCorrectTextAnswer())) {
                throw new TestQuestionValidationException("correctTextAnswer must be null for OPEN");
            }
        } else {
            throw new TestQuestionValidationException("Unsupported questionType: " + type);
        }
    }

    private TestQuestionType parseQuestionType(String raw, TestQuestionType defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return TestQuestionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new TestQuestionValidationException("Invalid questionType: " + raw + ". Supported: SINGLE_CHOICE, TEXT, OPEN");
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

    private boolean isRole(User user, RoleName role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role == user.getRole().getRolename();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
