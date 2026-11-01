package com.durga.employee_leave_management.service;

import com.durga.employee_leave_management.dto.LeaveApplicationDto;
import com.durga.employee_leave_management.dto.LeaveResponseDto;
import com.durga.employee_leave_management.dto.LeaveStatusUpdateDto;
import com.durga.employee_leave_management.model.LeaveStatus;

import java.util.List;

/**
 * Service interface for Leave Request workflows.
 */
public interface LeaveRequestService {

    LeaveResponseDto applyLeave(LeaveApplicationDto dto);

    List<LeaveResponseDto> getAllLeaveRequests(LeaveStatus status);

    LeaveResponseDto getLeaveRequestById(Long id);

    List<LeaveResponseDto> getLeaveRequestsByEmployee(Long employeeId);

    LeaveResponseDto updateLeaveStatus(Long id, LeaveStatusUpdateDto dto);
    
    LeaveResponseDto approveLeave(Long id, String adminRemarks);

    LeaveResponseDto rejectLeave(Long id, String adminRemarks);
}

