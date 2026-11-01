# Employee Leave Management System API
**ShadowFox Intermediate Java Developer Internship Project**

A production-ready, beginner-friendly RESTful API for managing employees and leave request workflows, built with **Spring Boot**, **Spring Data JPA**, and **MySQL**.

---

## 🌟 Key Features

1. **Employee Management (CRUD)**:
   - Register a new employee with unique email validation.
   - List all employees.
   - View employee details by ID.
   - Update employee details with duplicate email conflict protection.
   - Delete employee records (with cascade cleanup of associated leaves).
2. **Leave Request Management**:
   - Employees apply for leaves with leave type, start date, end date, and reason.
   - Support for multiple leave types: `SICK_LEAVE`, `CASUAL_LEAVE`, `ANNUAL_LEAVE`, `MATERNITY_LEAVE`, `PATERNITY_LEAVE`, `UNPAID_LEAVE`.
   - Automatic calculation of total leave days.
   - New requests start with status `PENDING`.
   - **Overlapping Leave Conflict Protection**: Prevents employees from submitting overlapping leave dates if they already have an active `PENDING` or `APPROVED` request.
3. **Admin Review & Decision Workflow**:
   - Approve pending leave requests with optional remarks.
   - Reject pending leave requests with rejection reasons.
   - Dedicated shortcut endpoints (`/api/leaves/{id}/approve` and `/api/leaves/{id}/reject`).
   - Prevent state mutations on already approved or rejected requests.
4. **Input Validation & Exception Handling**:
   - Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@NotNull`, `@Size`).
   - Centralized exception handling (`@RestControllerAdvice`) returning standardized JSON error responses.
   - Proper HTTP response codes (`201 Created`, `200 OK`, `204 No Content`, `400 Bad Request`, `404 Not Found`).
5. **Database Persistence & Security**:
   - MySQL persistence using Spring Data JPA / Hibernate with `ddl-auto=update`.
   - Credentials configured via environment variables (`SPRING_DATASOURCE_PASSWORD`) to keep secrets out of Git.
   - Isolated, fast in-memory H2 database for running unit and integration tests without touching MySQL.
   - 41 automated tests verifying all business rules, HTTP statuses, and end-to-end lifecycle.

---

## 🏗️ Layered Architecture Explained

This project strictly follows the industry-standard **Layered Architecture**. Each layer has a single responsibility:

```
[ HTTP Client / Postman / cURL ]
               │
               ▼
   1. Controller Layer (@RestController)
   ── Handles incoming HTTP requests, validates input DTOs, returns HTTP responses.
               │
               ▼
   2. Service Layer (@Service)
   ── Contains core business logic (date validation, overlap checks, status transition rules).
               │
               ▼
   3. Repository Layer (@Repository)
   ── Spring Data JPA interfaces that run SQL queries against the database without boilerplate.
               │
               ▼
   4. Entity / Model Layer (@Entity)
   ── Java classes mapped to relational database tables (employees, leave_requests).
```

### Additional Supporting Packages:
- **`dto` (Data Transfer Objects)**: Separates the database schema from the external API representation to protect internal models and customize payloads.
- **`exception`**: Centralized exception handling (`@RestControllerAdvice`) translating Java exceptions into clear JSON errors.

---

## 📁 Project Directory Structure

```
employee-leave-management/
├── .gitignore                                   # Protects secrets, target, and binaries
├── pom.xml                                      # Maven dependencies and build configuration
└── src/
    ├── main/
    │   ├── java/com/durga/employee_leave_management/
    │   │   ├── EmployeeLeaveManagementApplication.java  # Main Spring Boot entry point
    │   │   ├── controller/
    │   │   │   ├── EmployeeController.java      # /api/employees endpoints
    │   │   │   └── LeaveRequestController.java  # /api/leaves endpoints
    │   │   ├── dto/
    │   │   │   ├── EmployeeRequestDto.java      # Payload for creating/updating employees
    │   │   │   ├── EmployeeResponseDto.java     # Employee response payload
    │   │   │   ├── LeaveApplicationDto.java     # Payload for submitting leave
    │   │   │   ├── LeaveStatusUpdateDto.java    # Payload for approving/rejecting leave
    │   │   │   └── LeaveResponseDto.java        # Leave details response payload
    │   │   ├── exception/
    │   │   │   ├── BadRequestException.java     # 400 Business error
    │   │   │   ├── ResourceNotFoundException.java # 404 Not Found error
    │   │   │   ├── ErrorResponse.java           # Standardized error JSON model
    │   │   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice interceptor
    │   │   ├── model/
    │   │   │   ├── Employee.java                # JPA Employee entity
    │   │   │   ├── LeaveRequest.java            # JPA LeaveRequest entity
    │   │   │   ├── LeaveStatus.java             # PENDING, APPROVED, REJECTED
    │   │   │   └── LeaveType.java               # SICK_LEAVE, CASUAL_LEAVE, etc.
    │   │   ├── repository/
    │   │   │   ├── EmployeeRepository.java      # Spring Data JPA for Employee
    │   │   │   └── LeaveRequestRepository.java  # Spring Data JPA for LeaveRequest
    │   │   └── service/
    │   │       ├── EmployeeService.java         # Employee service interface
    │   │       ├── LeaveRequestService.java     # Leave service interface
    │   │       └── impl/
    │   │           ├── EmployeeServiceImpl.java
    │   │           └── LeaveRequestServiceImpl.java
    │   └── resources/
    │       └── application.properties           # MySQL runtime configuration
    └── test/
        ├── java/com/durga/employee_leave_management/
        │   ├── EmployeeLeaveManagementApplicationTests.java
        │   ├── controller/
        │   │   ├── EmployeeControllerIntegrationTest.java
        │   │   ├── LeaveRequestControllerIntegrationTest.java
        │   │   └── EndToEndWorkflowIntegrationTest.java # Full lifecycle integration test
        │   └── service/
        │       ├── EmployeeServiceTest.java
        │       └── LeaveRequestServiceTest.java
        └── resources/
            └── application.properties           # In-memory H2 configuration for tests
```

---

## ⚙️ Prerequisites & Setup

- **Java**: JDK 21 or higher (Verified with OpenJDK 21)
- **Maven**: Bundled Maven wrapper (`mvnw` / `mvnw.cmd`)
- **Database**: MySQL Server 8.0+ running on port `3306`

### 1. Database Configuration
In `src/main/resources/application.properties`, credentials use environment variable overrides with safe defaults:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/employee_leave_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

> **Setting your MySQL Password**:
> - If your local MySQL `root` user has a password (e.g. `secret123`), you can set it in PowerShell before starting the app:
>   ```powershell
>   $env:SPRING_DATASOURCE_PASSWORD="secret123"
>   ```
> - Or edit `spring.datasource.password=your_password` directly in your local `application.properties`.

---

## 🚀 Running the Application

Navigate to the project directory:
```powershell
cd "C:\Users\lochan m\Downloads\employee-leave-management\employee-leave-management"
```

### 1. Run All Tests (41 Tests)
```powershell
.\mvnw.cmd test
```
*Executes all 41 unit and integration tests against the isolated in-memory test database. No MySQL password needed for tests!*

### 2. Start the Spring Boot Server
```powershell
.\mvnw.cmd spring-boot:run
```
The application will start on `http://localhost:8080`.

### 3. Build Executable JAR
```powershell
.\mvnw.cmd clean package
java -jar target/employee-leave-management-0.0.1-SNAPSHOT.jar
```

---

## 📡 API Reference & Endpoints

### Employee Endpoints (`/api/employees`)

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/employees` | Create a new employee | `201 Created` |
| `GET` | `/api/employees` | Get all employees | `200 OK` |
| `GET` | `/api/employees/{id}` | Get employee by ID | `200 OK` / `404 Not Found` |
| `PUT` | `/api/employees/{id}` | Update employee details | `200 OK` / `400 Bad Request` |
| `DELETE` | `/api/employees/{id}` | Delete employee & cascade leaves | `204 No Content` |
| `GET` | `/api/employees/{id}/leaves` | Get all leaves for employee | `200 OK` |

### Leave Request Endpoints (`/api/leaves`)

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/leaves` | Apply for a leave (status PENDING) | `201 Created` |
| `GET` | `/api/leaves` | List all leaves (filter with `?status=PENDING`) | `200 OK` |
| `GET` | `/api/leaves/{id}` | Get leave request by ID | `200 OK` / `404 Not Found` |
| `GET` | `/api/leaves/employee/{id}` | Get all leaves for an employee | `200 OK` |
| `PUT` | `/api/leaves/{id}/status` | Admin review (APPROVE/REJECT) | `200 OK` / `400 Bad Request` |
| `PUT` | `/api/leaves/{id}/approve` | Shortcut: Approve leave | `200 OK` |
| `PUT` | `/api/leaves/{id}/reject` | Shortcut: Reject leave | `200 OK` |

---

## 🧪 Testing with cURL / PowerShell Examples

### 1. Create an Employee
```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Durga",
    "lastName": "Prasad",
    "email": "durga.prasad@example.com",
    "department": "Engineering",
    "designation": "Software Engineer"
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "firstName": "Durga",
  "lastName": "Prasad",
  "fullName": "Durga Prasad",
  "email": "durga.prasad@example.com",
  "department": "Engineering",
  "designation": "Software Engineer",
  "createdAt": "2026-10-26T20:49:05.123",
  "updatedAt": "2026-10-26T20:49:05.123"
}
```

### 2. List All Employees
```bash
curl -X GET http://localhost:8080/api/employees
```

### 3. Submit a Leave Request
```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "leaveType": "ANNUAL_LEAVE",
    "startDate": "2026-11-10",
    "endDate": "2026-11-14",
    "reason": "Annual family vacation"
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "Durga Prasad",
  "employeeEmail": "durga.prasad@example.com",
  "employeeDepartment": "Engineering",
  "leaveType": "ANNUAL_LEAVE",
  "startDate": "2026-11-10",
  "endDate": "2026-11-14",
  "totalDays": 5,
  "reason": "Annual family vacation",
  "status": "PENDING",
  "adminRemarks": null,
  "appliedAt": "2026-10-26T20:50:05.345",
  "reviewedAt": null
}
```

### 4. Admin Approves the Leave Request
```bash
curl -X PUT http://localhost:8080/api/leaves/1/approve \
  -H "Content-Type: application/json" \
  -d '{
    "adminRemarks": "Approved. Have a great vacation!"
  }'
```

**Response (200 OK):**
```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "Durga Prasad",
  "employeeEmail": "durga.prasad@example.com",
  "employeeDepartment": "Engineering",
  "leaveType": "ANNUAL_LEAVE",
  "startDate": "2026-11-10",
  "endDate": "2026-11-14",
  "totalDays": 5,
  "reason": "Annual family vacation",
  "status": "APPROVED",
  "adminRemarks": "Approved. Have a great vacation!",
  "appliedAt": "2026-10-26T20:50:05.345",
  "reviewedAt": "2026-10-26T20:51:10.789"
}
```

### 5. Admin Rejects a Leave Request
```bash
curl -X PUT http://localhost:8080/api/leaves/2/reject \
  -H "Content-Type: application/json" \
  -d '{
    "adminRemarks": "Project release scheduled during this period."
  }'
```

### 6. Filter Leave Requests by Status
```bash
curl -X GET "http://localhost:8080/api/leaves?status=PENDING"
```

### 7. Overlapping Date Validation (400 Bad Request)
If an employee attempts to apply for leave that overlaps with an existing `PENDING` or `APPROVED` leave:
```json
{
  "timestamp": "2026-10-26T20:52:06.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Leave request dates overlap with an existing APPROVED leave request (#1 from 2026-11-10 to 2026-11-14).",
  "path": "/api/leaves"
}
```
