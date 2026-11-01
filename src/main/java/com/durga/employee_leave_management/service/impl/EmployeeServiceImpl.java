package com.durga.employee_leave_management.service.impl;

import com.durga.employee_leave_management.dto.EmployeeRequestDto;
import com.durga.employee_leave_management.dto.EmployeeResponseDto;
import com.durga.employee_leave_management.exception.BadRequestException;
import com.durga.employee_leave_management.exception.ResourceNotFoundException;
import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation providing business logic for Employee management.
 */
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public EmployeeResponseDto createEmployee(EmployeeRequestDto dto) {
        // Check if an employee with the same email already exists
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("An employee with email '" + dto.getEmail() + "' already exists.");
        }

        Employee employee = new Employee(
                dto.getFirstName().trim(),
                dto.getLastName().trim(),
                dto.getEmail().trim().toLowerCase(),
                dto.getDepartment().trim(),
                dto.getDesignation().trim()
        );

        Employee savedEmployee = employeeRepository.save(employee);
        return mapToResponseDto(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return mapToResponseDto(employee);
    }

    @Override
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Check if updated email belongs to someone else
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new BadRequestException("Email '" + dto.getEmail() + "' is already in use by another employee.");
        }

        existingEmployee.setFirstName(dto.getFirstName().trim());
        existingEmployee.setLastName(dto.getLastName().trim());
        existingEmployee.setEmail(normalizedEmail);
        existingEmployee.setDepartment(dto.getDepartment().trim());
        existingEmployee.setDesignation(dto.getDesignation().trim());

        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        return mapToResponseDto(updatedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    /**
     * Helper method to convert Employee entity to EmployeeResponseDto.
     */
    private EmployeeResponseDto mapToResponseDto(Employee employee) {
        return new EmployeeResponseDto(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getDesignation(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
