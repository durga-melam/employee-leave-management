package com.durga.employee_leave_management.exception;

/**
 * Exception thrown when business validation fails (e.g., duplicate email, invalid dates, invalid status transition).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
