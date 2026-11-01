package com.durga.employee_leave_management.service;

import com.durga.employee_leave_management.dto.EmployeeRequestDto;
import com.durga.employee_leave_management.dto.EmployeeResponseDto;
import com.durga.employee_leave_management.exception.BadRequestException;
import com.durga.employee_leave_management.exception.ResourceNotFoundException;
import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeRequestDto requestDto;

    @BeforeEach
    void setUp() {
        employee = new Employee("Alice", "Smith", "alice@example.com", "Engineering", "Software Engineer");
        employee.setId(1L);

        requestDto = new EmployeeRequestDto("Alice", "Smith", "alice@example.com", "Engineering", "Software Engineer");
    }

    @Test
    @DisplayName("Should create employee successfully when email is unique")
    void testCreateEmployee_Success() {
        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDto result = employeeService.createEmployee(requestDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("Alice Smith", result.getFullName());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when creating employee with existing email")
    void testCreateEmployee_DuplicateEmail() {
        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                employeeService.createEmployee(requestDto));

        assertEquals("An employee with email 'alice@example.com' already exists.", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should get employee by ID successfully")
    void testGetEmployeeById_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponseDto result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("alice@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when employee ID does not exist")
    void testGetEmployeeById_NotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                employeeService.getEmployeeById(999L));
    }

    @Test
    @DisplayName("Should list all employees")
    void testGetAllEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<EmployeeResponseDto> list = employeeService.getAllEmployees();

        assertEquals(1, list.size());
        assertEquals("alice@example.com", list.get(0).getEmail());
    }

    @Test
    @DisplayName("Should update employee successfully")
    void testUpdateEmployee_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("alice@example.com", 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        requestDto.setDepartment("Platform Engineering");
        EmployeeResponseDto result = employeeService.updateEmployee(1L, requestDto);

        assertNotNull(result);
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Should delete employee successfully when ID exists")
    void testDeleteEmployee_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).delete(employee);
    }
}
