package com.durga.employee_leave_management.service;

import com.durga.employee_leave_management.dto.LeaveApplicationDto;
import com.durga.employee_leave_management.dto.LeaveResponseDto;
import com.durga.employee_leave_management.dto.LeaveStatusUpdateDto;
import com.durga.employee_leave_management.exception.BadRequestException;
import com.durga.employee_leave_management.exception.ResourceNotFoundException;
import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.model.LeaveRequest;
import com.durga.employee_leave_management.model.LeaveStatus;
import com.durga.employee_leave_management.model.LeaveType;
import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.repository.LeaveRequestRepository;
import com.durga.employee_leave_management.service.impl.LeaveRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private LeaveRequest pendingLeave;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        employee = new Employee("Bob", "Johnson", "bob@example.com", "Finance", "Analyst");
        employee.setId(10L);

        startDate = LocalDate.of(2026, 11, 1);
        endDate = LocalDate.of(2026, 11, 5);

        pendingLeave = new LeaveRequest(employee, LeaveType.ANNUAL_LEAVE, startDate, endDate, "Vacation");
        pendingLeave.setId(100L);
    }

    @Test
    @DisplayName("Should apply for leave successfully when employee exists and dates are valid")
    void testApplyLeave_Success() {
        LeaveApplicationDto dto = new LeaveApplicationDto(10L, LeaveType.ANNUAL_LEAVE, startDate, endDate, "Vacation");

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingLeave);

        LeaveResponseDto result = leaveRequestService.applyLeave(dto);

        assertNotNull(result);
        assertEquals(LeaveStatus.PENDING, result.getStatus());
        assertEquals(5, result.getTotalDays());
        assertEquals(10L, result.getEmployeeId());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when leave request overlaps with existing active leave")
    void testApplyLeave_OverlappingDates_Throws() {
        LeaveApplicationDto dto = new LeaveApplicationDto(10L, LeaveType.CASUAL_LEAVE, startDate, endDate, "Another leave");

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any(), any())).thenReturn(List.of(pendingLeave));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> leaveRequestService.applyLeave(dto));

        assertTrue(ex.getMessage().contains("dates overlap with an existing"));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when applying leave for non-existent employee")
    void testApplyLeave_EmployeeNotFound() {
        LeaveApplicationDto dto = new LeaveApplicationDto(999L, LeaveType.SICK_LEAVE, startDate, endDate, "Fever");

        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.applyLeave(dto));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when leave end date is before start date")
    void testApplyLeave_EndDateBeforeStartDate() {
        LeaveApplicationDto dto = new LeaveApplicationDto(
                10L,
                LeaveType.SICK_LEAVE,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 5),
                "Invalid dates"
        );

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                leaveRequestService.applyLeave(dto));

        assertTrue(ex.getMessage().contains("cannot be earlier than start date"));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Should approve a pending leave request successfully")
    void testUpdateLeaveStatus_Approve_Success() {
        LeaveStatusUpdateDto dto = new LeaveStatusUpdateDto(LeaveStatus.APPROVED, "Approved by Manager");

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveRequestService.updateLeaveStatus(100L, dto);

        assertNotNull(result);
        assertEquals(LeaveStatus.APPROVED, result.getStatus());
        assertEquals("Approved by Manager", result.getAdminRemarks());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    @DisplayName("Should approve leave using approveLeave shortcut method")
    void testApproveLeave_Shortcut() {
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveRequestService.approveLeave(100L, "Looks good");

        assertNotNull(result);
        assertEquals(LeaveStatus.APPROVED, result.getStatus());
        assertEquals("Looks good", result.getAdminRemarks());
    }

    @Test
    @DisplayName("Should reject a pending leave request successfully")
    void testUpdateLeaveStatus_Reject_Success() {
        LeaveStatusUpdateDto dto = new LeaveStatusUpdateDto(LeaveStatus.REJECTED, "Critical project deadline");

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveRequestService.updateLeaveStatus(100L, dto);

        assertNotNull(result);
        assertEquals(LeaveStatus.REJECTED, result.getStatus());
        assertEquals("Critical project deadline", result.getAdminRemarks());
    }

    @Test
    @DisplayName("Should reject leave using rejectLeave shortcut method")
    void testRejectLeave_Shortcut() {
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveRequestService.rejectLeave(100L, null);

        assertNotNull(result);
        assertEquals(LeaveStatus.REJECTED, result.getStatus());
        assertEquals("Rejected by administrator", result.getAdminRemarks());
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to change an already reviewed leave request")
    void testUpdateLeaveStatus_AlreadyReviewed() {
        pendingLeave.setStatus(LeaveStatus.APPROVED);
        LeaveStatusUpdateDto dto = new LeaveStatusUpdateDto(LeaveStatus.REJECTED, "Cannot reject now");

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                leaveRequestService.updateLeaveStatus(100L, dto));

        assertTrue(ex.getMessage().contains("already been APPROVED"));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when attempting to set status back to PENDING")
    void testUpdateLeaveStatus_RevertToPending_Throws() {
        LeaveStatusUpdateDto dto = new LeaveStatusUpdateDto(LeaveStatus.PENDING, "Reset to pending");

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingLeave));

        assertThrows(BadRequestException.class, () ->
                leaveRequestService.updateLeaveStatus(100L, dto));
    }

    @Test
    @DisplayName("Should list all leave requests for a given employee")
    void testGetLeaveRequestsByEmployee_Success() {
        when(employeeRepository.existsById(10L)).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeId(10L)).thenReturn(List.of(pendingLeave));

        List<LeaveResponseDto> result = leaveRequestService.getLeaveRequestsByEmployee(10L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }
}
