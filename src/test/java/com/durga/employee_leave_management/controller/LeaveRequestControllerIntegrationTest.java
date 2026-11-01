package com.durga.employee_leave_management.controller;

import com.durga.employee_leave_management.model.Employee;
import com.durga.employee_leave_management.model.LeaveRequest;
import com.durga.employee_leave_management.model.LeaveStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Employee employee;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        employeeRepository.deleteAll();

        employee = employeeRepository.save(
                new Employee("David", "Miller", "david.miller@example.com", "Operations", "Manager")
        );
    }

    @Test
    @DisplayName("POST /api/leaves should submit leave request and return 201 Created with PENDING status")
    void testApplyLeave_Success() throws Exception {
        String payload = """
                {
                    "employeeId": %d,
                    "leaveType": "CASUAL_LEAVE",
                    "startDate": "2026-11-01",
                    "endDate": "2026-11-03",
                    "reason": "Family function"
                }
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.leaveType", is("CASUAL_LEAVE")))
                .andExpect(jsonPath("$.totalDays", is(3)))
                .andExpect(jsonPath("$.employeeName", is("David Miller")));
    }

    @Test
    @DisplayName("POST /api/leaves with overlapping active dates should return 400 Bad Request")
    void testApplyLeave_OverlappingDates_Returns400() throws Exception {
        // Save initial leave
        leaveRequestRepository.save(new LeaveRequest(
                employee, LeaveType.ANNUAL_LEAVE,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), "Vacation"
        ));

        // Attempt to submit overlapping leave
        String overlappingPayload = """
                {
                    "employeeId": %d,
                    "leaveType": "SICK_LEAVE",
                    "startDate": "2026-11-12",
                    "endDate": "2026-11-14",
                    "reason": "Medical appointment"
                }
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("overlap")));
    }

    @Test
    @DisplayName("POST /api/leaves with invalid dates should return 400 Bad Request")
    void testApplyLeave_InvalidDates_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "employeeId": %d,
                    "leaveType": "CASUAL_LEAVE",
                    "startDate": "2026-11-10",
                    "endDate": "2026-11-05",
                    "reason": "Invalid date range"
                }
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot be earlier than start date")));
    }

    @Test
    @DisplayName("GET /api/leaves should return all leave requests")
    void testGetAllLeaves() throws Exception {
        LeaveRequest leave = new LeaveRequest(
                employee, LeaveType.SICK_LEAVE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2),
                "Doctor appointment"
        );
        leaveRequestRepository.save(leave);

        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].leaveType", is("SICK_LEAVE")));
    }

    @Test
    @DisplayName("GET /api/leaves?status=PENDING should filter leave requests by status")
    void testFilterLeavesByStatus() throws Exception {
        LeaveRequest pending = new LeaveRequest(
                employee, LeaveType.CASUAL_LEAVE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), "Personal"
        );
        pending.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(pending);

        LeaveRequest approved = new LeaveRequest(
                employee, LeaveType.ANNUAL_LEAVE,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), "Vacation"
        );
        approved.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(approved);

        mockMvc.perform(get("/api/leaves?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        mockMvc.perform(get("/api/leaves?status=APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("APPROVED")));
    }

    @Test
    @DisplayName("GET /api/leaves/{id} should return leave details")
    void testGetLeaveById() throws Exception {
        LeaveRequest leave = leaveRequestRepository.save(
                new LeaveRequest(employee, LeaveType.CASUAL_LEAVE,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), "Rest")
        );

        mockMvc.perform(get("/api/leaves/" + leave.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(leave.getId().intValue())))
                .andExpect(jsonPath("$.reason", is("Rest")));
    }

    @Test
    @DisplayName("GET /api/leaves/{id} with non-existent id should return 404 Not Found")
    void testGetLeaveById_NotFound() throws Exception {
        mockMvc.perform(get("/api/leaves/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/leaves/employee/{id} with non-existent employee should return 404 Not Found")
    void testGetLeavesByEmployee_NotFound() throws Exception {
        mockMvc.perform(get("/api/leaves/employee/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("PUT /api/leaves/{id}/approve should change status to APPROVED")
    void testApproveLeave() throws Exception {
        LeaveRequest leave = leaveRequestRepository.save(
                new LeaveRequest(employee, LeaveType.ANNUAL_LEAVE,
                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), "Vacation")
        );

        String approvePayload = """
                {
                    "status": "APPROVED",
                    "adminRemarks": "Approved by manager"
                }
                """;

        mockMvc.perform(put("/api/leaves/" + leave.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.adminRemarks", is("Approved by manager")));
    }

    @Test
    @DisplayName("PUT /api/leaves/{id}/reject should change status to REJECTED")
    void testRejectLeave() throws Exception {
        LeaveRequest leave = leaveRequestRepository.save(
                new LeaveRequest(employee, LeaveType.UNPAID_LEAVE,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), "Personal")
        );

        String rejectPayload = """
                {
                    "status": "REJECTED",
                    "adminRemarks": "Understaffed on those days"
                }
                """;

        mockMvc.perform(put("/api/leaves/" + leave.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.adminRemarks", is("Understaffed on those days")));
    }

    @Test
    @DisplayName("PUT /api/leaves/{id}/approve on already approved leave should return 400 Bad Request")
    void testApproveAlreadyApproved_Returns400() throws Exception {
        LeaveRequest leave = new LeaveRequest(employee, LeaveType.SICK_LEAVE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), "Medical");
        leave.setStatus(LeaveStatus.APPROVED);
        LeaveRequest saved = leaveRequestRepository.save(leave);

        mockMvc.perform(put("/api/leaves/" + saved.getId() + "/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already been APPROVED")));
    }
}
