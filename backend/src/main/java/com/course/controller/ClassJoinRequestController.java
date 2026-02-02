package com.course.controller;

import com.course.dto.ClassJoinRequestDto;
import com.course.dto.CreateClassJoinRequestDto;
import com.course.dto.UserDto;
import com.course.service.ClassJoinRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassJoinRequestController {

    private final ClassJoinRequestService joinRequestService;

    /**
     * Public endpoint (used by another service): submit a join request by class code.
     */
    @PostMapping("/api/join-requests")
    public ResponseEntity<ClassJoinRequestDto> create(@Valid @RequestBody CreateClassJoinRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(joinRequestService.createRequest(dto));
    }

    /**
     * TEACHER (responsible for class) or METHODIST (class creator): list requests.
     */
    @GetMapping("/api/classes/{classId}/join-requests")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<ClassJoinRequestDto>> listForClass(@PathVariable Integer classId) {
        return ResponseEntity.ok(joinRequestService.listForClass(classId));
    }

    /**
     * Approve request: creates STUDENT user, attaches to class and deletes request.
     */
    @PostMapping("/api/classes/{classId}/join-requests/{requestId}/approve")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<UserDto> approve(@PathVariable Integer classId, @PathVariable Integer requestId) {
        return ResponseEntity.ok(joinRequestService.approve(classId, requestId));
    }

    /**
     * Reject/delete request.
     */
    @DeleteMapping("/api/classes/{classId}/join-requests/{requestId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer classId, @PathVariable Integer requestId) {
        joinRequestService.delete(classId, requestId);
        return ResponseEntity.noContent().build();
    }
}
