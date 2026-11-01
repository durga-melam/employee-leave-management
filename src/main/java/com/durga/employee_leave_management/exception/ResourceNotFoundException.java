package com.durga.employee_leave_management.exception;

/**
 * Exception thrown when a requested resource (Employee, LeaveRequest) is not found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
