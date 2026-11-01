package com.durga.employee_leave_management.controller;

import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.model.LeaveRequest;
import com.durga.employee_leave_management.model.LeaveType;
import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/employees should create a new employee and return 201 Created")
    void testCreateEmployee_Success() throws Exception {
        String jsonPayload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "department": "Engineering",
                    "designation": "Backend Developer"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("POST /api/employees with duplicate email should return 400 Bad Request")
    void testCreateEmployee_DuplicateEmail_Returns400() throws Exception {
        employeeRepository.save(new Employee("Alice", "Smith", "duplicate@example.com", "HR", "Recruiter"));

        String duplicateJson = """
                {
                    "firstName": "Bob",
                    "lastName": "Smith",
                    "email": "duplicate@example.com",
                    "department": "Finance",
                    "designation": "Accountant"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/employees with invalid email should return 400 Bad Request")
    void testCreateEmployee_InvalidEmail_Returns400() throws Exception {
        String invalidJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "invalid-email-format",
                    "department": "Engineering",
                    "designation": "Backend Developer"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/employees with blank fields should return 400 Bad Request")
    void testCreateEmployee_BlankFields_Returns400() throws Exception {
        String blankJson = """
                {
                    "firstName": "",
                    "lastName": " ",
                    "email": "valid@example.com",
                    "department": "",
                    "designation": ""
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.firstName").exists());
    }

    @Test
    @DisplayName("GET /api/employees should return list of employees")
    void testGetAllEmployees() throws Exception {
        Employee emp = new Employee("Jane", "Doe", "jane.doe@example.com", "HR", "Manager");
        employeeRepository.save(emp);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("jane.doe@example.com")));
    }

    @Test
    @DisplayName("GET /api/employees/{id} should return employee details")
    void testGetEmployeeById() throws Exception {
        Employee emp = employeeRepository.save(new Employee("Mark", "Spencer", "mark@example.com", "Sales", "Executive"));

        mockMvc.perform(get("/api/employees/" + emp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(emp.getId().intValue())))
                .andExpect(jsonPath("$.email", is("mark@example.com")));
    }

    @Test
    @DisplayName("GET /api/employees/{id} with non-existent id should return 404 Not Found")
    void testGetEmployeeById_NotFound() throws Exception {
        mockMvc.perform(get("/api/employees/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} should update employee")
    void testUpdateEmployee() throws Exception {
        Employee emp = employeeRepository.save(new Employee("Sara", "Connor", "sara@example.com", "Security", "Lead"));

        String updateJson = """
                {
                    "firstName": "Sara",
                    "lastName": "Connor",
                    "email": "sara.updated@example.com",
                    "department": "Cyber Security",
                    "designation": "Director"
                }
                """;

        mockMvc.perform(put("/api/employees/" + emp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("sara.updated@example.com")))
                .andExpect(jsonPath("$.department", is("Cyber Security")));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} with another employee's email should return 400 Bad Request")
    void testUpdateEmployee_DuplicateEmail_Returns400() throws Exception {
        Employee emp1 = employeeRepository.save(new Employee("User1", "One", "user1@example.com", "Dept", "Role"));
        Employee emp2 = employeeRepository.save(new Employee("User2", "Two", "user2@example.com", "Dept", "Role"));

        String conflictingUpdate = """
                {
                    "firstName": "User2",
                    "lastName": "Two",
                    "email": "user1@example.com",
                    "department": "Dept",
                    "designation": "Role"
                }
                """;

        mockMvc.perform(put("/api/employees/" + emp2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictingUpdate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} should delete employee and cascade delete their leaves")
    void testDeleteEmployee_CascadesToLeaves() throws Exception {
        Employee emp = employeeRepository.save(new Employee("Temp", "User", "temp@example.com", "QA", "Tester"));
        LeaveRequest leave = leaveRequestRepository.save(new LeaveRequest(
                emp, LeaveType.CASUAL_LEAVE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Personal"
        ));

        mockMvc.perform(delete("/api/employees/" + emp.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/" + emp.getId()))
                .andExpect(status().isNotFound());

        assertTrue(leaveRequestRepository.findById(leave.getId()).isEmpty());
    }
}
