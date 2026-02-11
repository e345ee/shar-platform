package com.course.controller;

import com.course.dto.classroom.ClassJoinRequestByCodeRequest;
import com.course.dto.classroom.ClassJoinRequestResponse;
import com.course.dto.user.UserResponse;
import com.course.service.ClassJoinRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/join-requests")
@RequiredArgsConstructor
public class JoinRequestsController {

    private final ClassJoinRequestService joinRequestService;

    
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ClassJoinRequestResponse> create(@Valid @RequestBody ClassJoinRequestByCodeRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(joinRequestService.createRequest(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<ClassJoinRequestResponse>> listForClass(@RequestParam Integer classId) {
        return ResponseEntity.ok(joinRequestService.listForClass(classId));
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<UserResponse> approve(@PathVariable Integer requestId, @RequestParam Integer classId) {
        return ResponseEntity.ok(joinRequestService.approve(classId, requestId));
    }

    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer requestId, @RequestParam Integer classId) {
        joinRequestService.delete(classId, requestId);
        return ResponseEntity.noContent().build();
    }
}
