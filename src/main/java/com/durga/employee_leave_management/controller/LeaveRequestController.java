package com.durga.employee_leave_management.controller;

import com.durga.employee_leave_management.dto.LeaveApplicationDto;
import com.durga.employee_leave_management.dto.LeaveResponseDto;
import com.durga.employee_leave_management.dto.LeaveStatusUpdateDto;
import com.durga.employee_leave_management.model.LeaveStatus;
import com.durga.employee_leave_management.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller exposing Leave Request operations and Admin approval/rejection workflows.
 * Base URL: /api/leaves
 */
@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    /**
     * Submit a new leave request.
     * HTTP 201 CREATED
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<LeaveResponseDto> applyLeave(@Valid @RequestBody LeaveApplicationDto dto) {
        LeaveResponseDto response = leaveRequestService.applyLeave(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all leave requests, optionally filtered by status (e.g. ?status=PENDING).
     * HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves(
            @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests(status));
    }

    /**
     * Get a specific leave request by ID.
     * HTTP 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponseDto> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id));
    }

    /**
     * Get all leave requests for a given employee ID.
     * HTTP 200 OK
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponseDto>> getLeavesByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId));
    }

    /**
     * Admin review: Approve or Reject a leave request.
     * HTTP 200 OK
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<LeaveResponseDto> updateLeaveStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeaveStatusUpdateDto dto) {
        return ResponseEntity.ok(leaveRequestService.updateLeaveStatus(id, dto));
    }

    /**
     * Convenience shortcut to approve a pending leave request.
     * HTTP 200 OK
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveResponseDto> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveStatusUpdateDto dto) {
        String remarks = (dto != null) ? dto.getAdminRemarks() : null;
        return ResponseEntity.ok(leaveRequestService.approveLeave(id, remarks));
    }

    /**
     * Convenience shortcut to reject a pending leave request.
     * HTTP 200 OK
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveResponseDto> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveStatusUpdateDto dto) {
        String remarks = (dto != null) ? dto.getAdminRemarks() : null;
        return ResponseEntity.ok(leaveRequestService.rejectLeave(id, remarks));
    }
}
