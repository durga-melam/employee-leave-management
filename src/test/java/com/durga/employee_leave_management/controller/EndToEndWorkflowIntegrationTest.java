package com.durga.employee_leave_management.controller;

import com.durga.employee_leave_management.repository.EmployeeRepository;
import com.durga.employee_leave_management.repository.LeaveRequestRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class EndToEndWorkflowIntegrationTest {

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
    @DisplayName("Complete end-to-end lifecycle test for Employee Leave Management System")
    void testCompleteEmployeeLeaveLifecycle() throws Exception {
        // Step 1: Create Employee (POST /api/employees)
        String createEmployeeJson = """
                {
                    "firstName": "Durga",
                    "lastName": "Prasad",
                    "email": "durga.prasad@example.com",
                    "department": "Engineering",
                    "designation": "Associate Software Engineer"
                }
                """;

        MvcResult createEmployeeResult = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email", is("durga.prasad@example.com")))
                .andExpect(jsonPath("$.fullName", is("Durga Prasad")))
                .andReturn();

        Number employeeIdNum = JsonPath.read(createEmployeeResult.getResponse().getContentAsString(), "$.id");
        Long employeeId = employeeIdNum.longValue();

        // Step 2: Retrieve Employee by ID (GET /api/employees/{id})
        mockMvc.perform(get("/api/employees/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employeeId.intValue())))
                .andExpect(jsonPath("$.department", is("Engineering")));

        // Step 3: List all Employees (GET /api/employees)
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("durga.prasad@example.com")));

        // Step 4: Update Employee details (PUT /api/employees/{id})
        String updateEmployeeJson = """
                {
                    "firstName": "Durga",
                    "lastName": "Prasad",
                    "email": "durga.prasad@example.com",
                    "department": "Engineering",
                    "designation": "Senior Software Engineer"
                }
                """;

        mockMvc.perform(put("/api/employees/" + employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateEmployeeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation", is("Senior Software Engineer")));

        // Step 5: Submit Leave Request 1 (POST /api/leaves)
        String leave1Json = """
                {
                    "employeeId": %d,
                    "leaveType": "ANNUAL_LEAVE",
                    "startDate": "2026-11-10",
                    "endDate": "2026-11-14",
                    "reason": "Annual family holiday"
                }
                """.formatted(employeeId);

        MvcResult leave1Result = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leave1Json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.leaveType", is("ANNUAL_LEAVE")))
                .andExpect(jsonPath("$.totalDays", is(5)))
                .andExpect(jsonPath("$.employeeName", is("Durga Prasad")))
                .andReturn();

        Number leave1IdNum = JsonPath.read(leave1Result.getResponse().getContentAsString(), "$.id");
        Long leave1Id = leave1IdNum.longValue();

        // Step 6: View Leave Request by ID (GET /api/leaves/{id})
        mockMvc.perform(get("/api/leaves/" + leave1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(leave1Id.intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));

        // Step 7: View Leaves for Employee (GET /api/employees/{id}/leaves)
        mockMvc.perform(get("/api/employees/" + employeeId + "/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(leave1Id.intValue())));

        // Step 8: Admin Approves Leave 1 (PUT /api/leaves/{id}/approve)
        String approvalJson = """
                {
                    "adminRemarks": "Approved. Have a great vacation!"
                }
                """;

        mockMvc.perform(put("/api/leaves/" + leave1Id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.adminRemarks", is("Approved. Have a great vacation!")))
                .andExpect(jsonPath("$.reviewedAt").exists());

        // Step 9: Verify Cannot Re-approve or Reject already approved leave (PUT /api/leaves/{id}/approve -> 400)
        mockMvc.perform(put("/api/leaves/" + leave1Id + "/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already been APPROVED")));

        // Step 10: Attempt to submit overlapping leave request -> 400 Bad Request
        String overlappingLeaveJson = """
                {
                    "employeeId": %d,
                    "leaveType": "CASUAL_LEAVE",
                    "startDate": "2026-11-12",
                    "endDate": "2026-11-13",
                    "reason": "Overlapping request"
                }
                """.formatted(employeeId);

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingLeaveJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("overlap")));

        // Step 11: Submit Leave Request 2 on non-overlapping dates (POST /api/leaves)
        String leave2Json = """
                {
                    "employeeId": %d,
                    "leaveType": "SICK_LEAVE",
                    "startDate": "2026-11-20",
                    "endDate": "2026-11-21",
                    "reason": "Doctor appointment"
                }
                """.formatted(employeeId);

        MvcResult leave2Result = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leave2Json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        Number leave2IdNum = JsonPath.read(leave2Result.getResponse().getContentAsString(), "$.id");
        Long leave2Id = leave2IdNum.longValue();

        // Step 12: Admin Rejects Leave 2 (PUT /api/leaves/{id}/reject)
        String rejectionJson = """
                {
                    "adminRemarks": "Crucial sprint demo scheduled on this date"
                }
                """;

        mockMvc.perform(put("/api/leaves/" + leave2Id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.adminRemarks", is("Crucial sprint demo scheduled on this date")));

        // Step 13: Filter leaves by status
        mockMvc.perform(get("/api/leaves?status=APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(leave1Id.intValue())));

        mockMvc.perform(get("/api/leaves?status=REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(leave2Id.intValue())));

        mockMvc.perform(get("/api/leaves?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 14: Delete Employee and verify cascade cleanup (DELETE /api/employees/{id})
        mockMvc.perform(delete("/api/employees/" + employeeId))
                .andExpect(status().isNoContent());

        // Employee should no longer exist
        mockMvc.perform(get("/api/employees/" + employeeId))
                .andExpect(status().isNotFound());

        // Associated leaves should be removed automatically
        assertTrue(leaveRequestRepository.findById(leave1Id).isEmpty());
        assertTrue(leaveRequestRepository.findById(leave2Id).isEmpty());
    }
}
