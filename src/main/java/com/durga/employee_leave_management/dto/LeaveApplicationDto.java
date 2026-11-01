package com.durga.employee_leave_management.dto;

import com.durga.employee_leave_management.model.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for applying / submitting a new Leave Request.
 */
public class LeaveApplicationDto {

    @NotNull(message = "Employee ID is mandatory")
    private Long employeeId;

    @NotNull(message = "Leave type is mandatory (e.g., SICK_LEAVE, CASUAL_LEAVE, ANNUAL_LEAVE)")
    private LeaveType leaveType;

    @NotNull(message = "Start date is mandatory (format: YYYY-MM-DD)")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory (format: YYYY-MM-DD)")
    private LocalDate endDate;

    @NotBlank(message = "Reason for leave is mandatory")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    private String reason;

    public LeaveApplicationDto() {
    }

    public LeaveApplicationDto(Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
