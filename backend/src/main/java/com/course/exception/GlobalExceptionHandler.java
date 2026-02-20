package com.course.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<Class<?>, String> RU_MESSAGE_BY_EXCEPTION = Map.ofEntries(
            Map.entry(AchievementAccessDeniedException.class, "Недостаточно прав для работы с достижением."),
            Map.entry(AchievementAlreadyAwardedException.class, "Достижение уже выдано."),
            Map.entry(AchievementNotFoundException.class, "Достижение не найдено."),
            Map.entry(AchievementPhotoValidationException.class, "Фото достижения не прошло проверку."),
            Map.entry(AchievementValidationException.class, "Некорректные данные достижения."),

            Map.entry(ClassNotFoundException.class, "Класс не найден."),
            Map.entry(ClassStudentAccessDeniedException.class, "Недостаточно прав для доступа к классу."),
            Map.entry(CourseNotClosedException.class, "Курс ещё не завершён."),

            Map.entry(InvalidEmailException.class, "Некорректный адрес электронной почты."),
            Map.entry(LessonAccessDeniedException.class, "Недостаточно прав для доступа к уроку."),
            Map.entry(LessonNotFoundException.class, "Урок не найден."),
            Map.entry(LessonPresentationNotFoundException.class, "Презентация урока не найдена."),
            Map.entry(LessonPresentationValidationException.class, "Некорректные данные презентации урока."),
            Map.entry(LessonValidationException.class, "Некорректные данные урока."),

            Map.entry(MailNotConfiguredException.class, "Отправка писем не настроена."),
            Map.entry(MailSendingException.class, "Не удалось отправить письмо. Попробуйте позже."),

            Map.entry(NotificationNotFoundException.class, "Уведомление не найдено."),
            Map.entry(ResourceNotFoundException.class, "Ресурс не найден."),
            Map.entry(StudentNotEnrolledInClassException.class, "Студент не записан в этот класс."),
            Map.entry(TeacherDeletionConflictException.class, "Нельзя удалить преподавателя: есть связанные данные."),

            Map.entry(TestAccessDeniedException.class, "Недостаточно прав для доступа к тесту."),
            Map.entry(TestAttemptAccessDeniedException.class, "Недостаточно прав для доступа к попытке теста."),
            Map.entry(TestAttemptNotFoundException.class, "Попытка теста не найдена."),
            Map.entry(TestAttemptTimeLimitExceededException.class, "Время на выполнение теста истекло."),
            Map.entry(TestAttemptValidationException.class, "Некорректные данные попытки теста."),
            Map.entry(TestNotFoundException.class, "Тест не найден."),
            Map.entry(TestQuestionNotFoundException.class, "Вопрос теста не найден."),
            Map.entry(TestQuestionValidationException.class, "Некорректные данные вопроса теста."),
            Map.entry(TestValidationException.class, "Некорректные данные теста."),

            Map.entry(DuplicateResourceException.class, "Такая запись уже существует."),
            Map.entry(ForbiddenOperationException.class, "Операция запрещена."),
            Map.entry(IllegalArgumentException.class, "Некорректные данные.")
    );

    private static boolean containsLatin(String value) {
        if (value == null || value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                return true;
            }
        }
        return false;
    }

    private static String ruReasonPhrase(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Некорректный запрос";
            case FORBIDDEN -> "Запрещено";
            case NOT_FOUND -> "Не найдено";
            case CONFLICT -> "Конфликт";
            case INTERNAL_SERVER_ERROR -> "Ошибка сервера";
            default -> "Ошибка";
        };
    }

    private static String resolveRuMessage(Throwable ex, String fallbackRu) {
        if (ex == null) return fallbackRu;

        String original = ex.getMessage();
        if (original != null && !original.isBlank() && !containsLatin(original)) {
            return original;
        }

        String mapped = RU_MESSAGE_BY_EXCEPTION.get(ex.getClass());
        if (mapped != null) return mapped;

        for (var entry : RU_MESSAGE_BY_EXCEPTION.entrySet()) {
            if (entry.getKey().isAssignableFrom(ex.getClass())) {
                return entry.getValue();
            }
        }

        return fallbackRu;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(ruReasonPhrase(HttpStatus.NOT_FOUND))
                .message(resolveRuMessage(ex, "Ресурс не найден."))
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenOperationException ex,
                                                            HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(ruReasonPhrase(HttpStatus.FORBIDDEN))
                .message(resolveRuMessage(ex, "Недостаточно прав для выполнения операции."))
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex,
                                                            HttpServletRequest request) {

        String msg = ex.getMessage();
        String field = null;
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("tg") && lower.contains("id")) {
                field = "tgId";
            } else if (lower.contains("email")) {
                field = "email";
            } else if (lower.contains("name")) {
                field = "name";
            } else if (lower.contains("title")) {
                field = "title";
            }
        }

        if (msg == null || msg.isBlank() || containsLatin(msg)) {
            if ("email".equals(field)) {
                msg = "Этот адрес электронной почты уже используется.";
            } else if ("tgId".equals(field)) {
                msg = "Этот идентификатор Телеграм уже используется.";
            } else if ("name".equals(field)) {
                msg = "Такое значение поля «Имя» уже используется.";
            } else if ("title".equals(field)) {
                msg = "Такое значение поля «Название» уже используется.";
            } else {
                msg = resolveRuMessage(ex, "Такая запись уже существует.");
            }
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(ruReasonPhrase(HttpStatus.CONFLICT))
                .message(msg)
                .path(request.getRequestURI())
                .violations(List.of(ApiErrorResponse.Violation.builder()
                        .field(field)
                        .message(msg)
                        .code("Duplicate")
                        .build()))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        List<ApiErrorResponse.Violation> violations = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            violations.add(ApiErrorResponse.Violation.builder()
                    .field(fe.getField())
                    .message(fe.getDefaultMessage())
                    .code(fe.getCode())
                    .build());
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ruReasonPhrase(HttpStatus.BAD_REQUEST))
                .message("Ошибка валидации данных")
                .path(request.getRequestURI())
                .violations(violations)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<ApiErrorResponse.Violation> violations = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String field = v.getPropertyPath() == null ? null : v.getPropertyPath().toString();
            violations.add(ApiErrorResponse.Violation.builder()
                    .field(field)
                    .message(v.getMessage())
                    .code(v.getConstraintDescriptor() == null ? null : v.getConstraintDescriptor()
                            .getAnnotation().annotationType().getSimpleName())
                    .build());
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ruReasonPhrase(HttpStatus.BAD_REQUEST))
                .message("Ошибка валидации данных")
                .path(request.getRequestURI())
                .violations(violations)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ruReasonPhrase(HttpStatus.BAD_REQUEST))
                .message(resolveRuMessage(ex, "Некорректные данные."))
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {

        String msg = "Нарушено ограничение базы данных";
        String field = null;
        String code = "DbConstraint";

        String rootMsg = null;
        Throwable root = ex.getMostSpecificCause();
        if (root != null) {
            rootMsg = root.getMessage();
        }

        if (rootMsg != null) {
            String m = rootMsg;
            if (m.contains("ux_users_email_lower") || m.contains("users_email_key")) {
                field = "email";
                msg = "Адрес электронной почты должен быть уникальным";
                code = "Unique";
            } else if (m.contains("users_name_key")) {
                field = "name";
                msg = "Имя должно быть уникальным";
                code = "Unique";
            } else if (m.contains("uq_class_name_in_course")) {
                field = "name";
                msg = "Название класса должно быть уникальным в рамках курса";
                code = "Unique";
            } else if (m.contains("uq_lesson_title_in_course")) {
                field = "title";
                msg = "Название урока должно быть уникальным в рамках курса";
                code = "Unique";
            } else if (m.contains("uq_lesson_order_in_course") || m.contains("uq_test_question_order")) {
                field = "orderIndex";
                msg = "Порядковый номер должен быть уникальным в рамках родителя";
                code = "Unique";
            } else {
                msg = containsLatin(rootMsg) ? "Нарушено ограничение базы данных" : rootMsg;
            }
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(ruReasonPhrase(HttpStatus.CONFLICT))
                .message(msg)
                .path(request.getRequestURI())
                .violations(List.of(ApiErrorResponse.Violation.builder()
                        .field(field)
                        .message(msg)
                        .code(code)
                        .build()))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAny(Exception ex,
                                                      HttpServletRequest request) {
        log.error("Необработанное исключение", ex);
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(ruReasonPhrase(HttpStatus.INTERNAL_SERVER_ERROR))
                .message("Внутренняя ошибка сервера. Попробуйте позже.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
