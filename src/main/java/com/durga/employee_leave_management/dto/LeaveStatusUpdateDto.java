package com.durga.employee_leave_management.dto;

import com.durga.employee_leave_management.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for Admin reviewing (approving or rejecting) a leave request.
 */
public class LeaveStatusUpdateDto {

    @NotNull(message = "Status is mandatory (APPROVED or REJECTED)")
    private LeaveStatus status;

    @Size(max = 500, message = "Admin remarks cannot exceed 500 characters")
    private String adminRemarks;

    public LeaveStatusUpdateDto() {
    }

    public LeaveStatusUpdateDto(LeaveStatus status, String adminRemarks) {
        this.status = status;
        this.adminRemarks = adminRemarks;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }
}
