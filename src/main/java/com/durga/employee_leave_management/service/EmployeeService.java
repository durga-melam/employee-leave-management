package com.durga.employee_leave_management.service;

import com.durga.employee_leave_management.dto.EmployeeRequestDto;
import com.durga.employee_leave_management.dto.EmployeeResponseDto;

import java.util.List;

/**
 * Service interface for Employee operations.
 */
public interface EmployeeService {

    EmployeeResponseDto createEmployee(EmployeeRequestDto dto);

    List<EmployeeResponseDto> getAllEmployees();

    EmployeeResponseDto getEmployeeById(Long id);

    EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto);

    void deleteEmployee(Long id);
}
