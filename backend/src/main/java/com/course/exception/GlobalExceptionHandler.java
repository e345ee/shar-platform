package com.course.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
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
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
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

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
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
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
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
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
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
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        
        String msg = "Database constraint violation";
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
                msg = "Email must be unique";
                code = "Unique";
            } else if (m.contains("users_name_key")) {
                field = "name";
                msg = "Name must be unique";
                code = "Unique";
            } else if (m.contains("uq_class_name_in_course")) {
                field = "name";
                msg = "Class name must be unique within the course";
                code = "Unique";
            } else if (m.contains("uq_lesson_title_in_course")) {
                field = "title";
                msg = "Lesson title must be unique within the course";
                code = "Unique";
            } else if (m.contains("uq_lesson_order_in_course") || m.contains("uq_test_question_order")) {
                field = "orderIndex";
                msg = "Order index must be unique within the parent";
                code = "Unique";
            } else {
                
                msg = rootMsg;
            }
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
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
}
