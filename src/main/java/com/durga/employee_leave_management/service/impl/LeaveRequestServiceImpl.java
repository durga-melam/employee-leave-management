package com.durga.employee_leave_management.service.impl;

import com.durga.employee_leave_management.dto.LeaveApplicationDto;
import com.durga.employee_leave_management.dto.LeaveResponseDto;
import com.durga.employee_leave_management.dto.LeaveStatusUpdateDto;
import com.durga.employee_leave_management.exception.BadRequestException;
import com.durga.employee_leave_management.exception.ResourceNotFoundException;
import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.model.LeaveRequest;
import com.durga.employee_leave_management.model.LeaveStatus;
import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.repository.LeaveRequestRepository;
import com.durga.employee_leave_management.service.LeaveRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation providing business logic for Leave Request lifecycle.
 */
@Service
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestServiceImpl(LeaveRequestRepository leaveRequestRepository,
                                  EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public LeaveResponseDto applyLeave(LeaveApplicationDto dto) {
        // Validate Employee existence
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot apply leave: Employee not found with id: " + dto.getEmployeeId()));

        // Validate Date Range
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("Leave end date (" + dto.getEndDate() +
                    ") cannot be earlier than start date (" + dto.getStartDate() + ").");
        }

        // Validate for overlapping active leave requests (PENDING or APPROVED)
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlappingRequests(
                employee.getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );
        if (!overlaps.isEmpty()) {
            LeaveRequest conflict = overlaps.get(0);
            throw new BadRequestException("Leave request dates overlap with an existing " +
                    conflict.getStatus() + " leave request (#" + conflict.getId() +
                    " from " + conflict.getStartDate() + " to " + conflict.getEndDate() + ").");
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                dto.getLeaveType(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getReason().trim()
        );

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getAllLeaveRequests(LeaveStatus status) {
        List<LeaveRequest> list = (status != null)
                ? leaveRequestRepository.findByStatus(status)
                : leaveRequestRepository.findAll();

        return list.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponseDto getLeaveRequestById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
        return mapToResponseDto(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getLeaveRequestsByEmployee(Long employeeId) {
        // Ensure employee exists
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public LeaveResponseDto updateLeaveStatus(Long id, LeaveStatusUpdateDto dto) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        // Validate target status
        if (dto.getStatus() == LeaveStatus.PENDING) {
            throw new BadRequestException("Cannot set status back to PENDING. Must be APPROVED or REJECTED.");
        }

        // Validate current status
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Action rejected: Leave request #" + id +
                    " has already been " + leaveRequest.getStatus() + ".");
        }

        leaveRequest.setStatus(dto.getStatus());
        if (dto.getAdminRemarks() != null && !dto.getAdminRemarks().trim().isEmpty()) {
            leaveRequest.setAdminRemarks(dto.getAdminRemarks().trim());
        }
        leaveRequest.setReviewedAt(LocalDateTime.now());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return mapToResponseDto(updated);
    }

    @Override
    public LeaveResponseDto approveLeave(Long id, String adminRemarks) {
        String remarks = (adminRemarks != null && !adminRemarks.isBlank())
                ? adminRemarks.trim()
                : "Approved by administrator";
        return updateLeaveStatus(id, new LeaveStatusUpdateDto(LeaveStatus.APPROVED, remarks));
    }

    @Override
    public LeaveResponseDto rejectLeave(Long id, String adminRemarks) {
        String remarks = (adminRemarks != null && !adminRemarks.isBlank())
                ? adminRemarks.trim()
                : "Rejected by administrator";
        return updateLeaveStatus(id, new LeaveStatusUpdateDto(LeaveStatus.REJECTED, remarks));
    }

    /**
     * Helper to map LeaveRequest entity to LeaveResponseDto.
     */
    private LeaveResponseDto mapToResponseDto(LeaveRequest leave) {
        long totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        Employee emp = leave.getEmployee();

        return new LeaveResponseDto(
                leave.getId(),
                emp != null ? emp.getId() : null,
                emp != null ? emp.getFirstName() + " " + emp.getLastName() : "Unknown",
                emp != null ? emp.getEmail() : null,
                emp != null ? emp.getDepartment() : null,
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                totalDays,
                leave.getReason(),
                leave.getStatus(),
                leave.getAdminRemarks(),
                leave.getAppliedAt(),
                leave.getReviewedAt()
        );
    }
}
