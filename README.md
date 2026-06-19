# Task Manager API

A Spring Boot RESTful API for managing tasks, projects, and team collaboration - built as a learning project to demonstrate backend development skills.

## 📋 Overview

This project implements a comprehensive task management system with user authentication, project organization, task tracking, and commenting features. Built with Spring Boot 3.x and Java 21, it follows enterprise-grade architectural patterns while focusing on clean code, proper documentation, and production-ready practices.

### Key Learning Outcomes
- **Layered Architecture**: Implemented clean separation of concerns (Controller → Service → Repository)
- **RESTful Design**: Created intuitive, consistent API endpoints following REST principles
- **API Documentation**: Integrated SpringDoc OpenAPI for interactive Swagger documentation
- **Security**: Implemented JWT-based authentication with role-based access considerations
- **Configuration Management**: Utilized environment variables for environment-specific settings
- **Data Validation**: Applied Bean Validation (JSR 380) for robust input handling
- **Error Handling**: Implemented global exception handling for consistent API responses
- **Database Integration**: Used Spring Data JPA with PostgreSQL for persistent storage

## 🏗️ Project Structure

```
task-manager/
├── .env                  # Environment variables (not committed)
├── docker-compose.yml    # Docker Compose definition
├── backend/              # Spring Boot application
│   ├── Dockerfile        # Docker image definition
│   ├── pom.xml           # Maven dependencies and build configuration
│   └── src/
│       └── main/
│           ├── java/com/thanh/taskmanager/
│           │   ├── config/         # Global configurations (Security, OpenAPI, CORS)
│           │   ├── controller/     # REST API endpoints (Auth, Project, Task, Comment, Profile)
│           │   ├── dto/            # Data Transfer Objects (request/response models)
│           │   ├── entity/         # JPA entities mapped to database tables
│           │   ├── exception/      # Custom exceptions and global handler
│           │   ├── mapper/         # Object mapping between entities and DTOs
│           │   ├── repository/     # Spring Data JPA interfaces
│           │   ├── security/       # JWT filters, custom user details
│           │   ├── service/        # Business logic implementation
│           │   └── utils/          # Utility and helper classes
│           └── resources/
│               ├── application.yaml    # Base configuration
│               ├── application-dev.yaml # Development overrides
│               └── application-docker.yaml # Docker-specific config
└── README.md
```

## 🔧 Features Implemented

### Core Functionality
- **User Authentication**: Registration, login, token refresh (JWT)
- **Profile Management**: Retrieve and update user information, password change
- **Project CRUD**: Create, read, update, delete projects with membership management
- **Task CRUD**: Full task lifecycle within projects (creation, assignment, status updates)
- **Commenting System**: Add, retrieve, update, delete comments on tasks
- **Project Membership**: Add/remove users from projects with permission checks

### Technical Features
- **RESTful API Design**: Consistent resource-based endpoints with proper HTTP methods
- **API Documentation**: Interactive Swagger UI at `/swagger-ui.html`
- **Input Validation**: Comprehensive validation using Hibernate Validator annotations
- **Global Error Handling**: Centralized exception handling with meaningful error responses
- **Pagination Support**: For task listing with filtering capabilities
- **Security**: JWT authentication with token expiration and refresh mechanisms
- **CORS Configuration**: Enables cross-origin requests for frontend integration
- **Environment Management**: `.env` file handling via spring-dotenv for different environments

## 📚 Technical Decisions & Rationale

### Architecture Choices
- **Layered Architecture**: Separated concerns to improve maintainability and testability
  - Controllers handle HTTP concerns only (routing, validation, response formatting)
  - Services contain business logic and transaction boundaries
  - Repositories focus solely on data access
  - Mappers handle object transformation between layers
- **DTO Pattern**: Prevents entity exposure and allows tailored API responses
- **Service Layer Transactions**: `@Transactional` at service level for proper rollback behavior

### API Design Principles
- **Resource-Oriented**: Endpoints modeled after business entities (projects, tasks, etc.)
- **Consistent Naming**: Verb-noun pattern (`/projects`, `/projects/{id}`) with HTTP methods indicating action
- **Status Codes**: Proper use of HTTP status codes (200, 201, 204, 400, 401, 403, 404)
- **Response Wrapping**: All responses wrapped in standardized `AppResponse` format
- **Path Variables**: Used for resource identifiers (`{projectId}`, `{taskId}`)
- **Request Bodies**: For complex data creation/update operations
- **Query Parameters**: For filtering, sorting, and pagination (where applicable)

### Security Implementation
- **Stateless Authentication**: JWT tokens stored client-side, verified on each request
- **Token Separation**: Access tokens (short-lived) and refresh tokens (long-lived)
- **Password Security**: BCrypt hashing for stored passwords
- **Endpoint Protection**: Secured endpoints via Spring Security filter chain
- **Current User Extraction**: Utility method for accessing authenticated user ID across layers

### Documentation Approach
- **Code-First Documentation**: OpenAPI annotations directly in controller methods
- **Comprehensive Coverage**: Every endpoint documented with:
  - Clear summaries and descriptions
  - Parameter descriptions (path, query, body)
  - Response examples with appropriate status codes
  - Error condition documentation
- **Interactive Testing**: Swagger UI enables direct API testing without external tools

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK (if running via Maven)
- Maven 3.8+ (if running via Maven)
- Docker & Docker Compose
- (Optional) PostgreSQL 13+ (if not using Docker)

### Environment Setup
1. Clone the repository
2. Create `.env` file in project root (copy from example below)
3. Choose how to run the application:
   - **Option 1: Docker Compose (Recommended)**
     ```bash
     docker-compose up --build
     ```
     This will start PostgreSQL and the Spring Boot API.
   - **Option 2: Maven (Local Development)**
     Ensure PostgreSQL is running and accessible, then:
     ```bash
     cd backend
     $env:DOTENV_FILE="..\.env"  # PowerShell (Windows)
     # or
     export DOTENV_FILE=../.env  # Bash (Linux/Mac)
     ./mvnw spring-boot:run
     ```

### Environment Variables (`.env`)
```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=task_manager_db
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password

# Application Configuration
APP_PORT=8081
SECRET_KEY=your_256_bit_secret_key_here_make_it_strong
TOKEN_EXP=900000          # 15 minutes in milliseconds
REFRESH_EXP=604800000     # 7 days in milliseconds
```

### Application Startup
When using Docker Compose, the API will be available at `http://localhost:8081` (port mapped from `${APP_PORT:-8081}:8081`).
When running via Maven, the API will also start on `http://localhost:8081`.
API endpoints are prefixed by `/api/v1`.

### API Documentation
Once running, access:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs
- **OpenAPI YAML**: http://localhost:8081/v3/api-docs.yaml

## 🧪 Testing the API

### Authentication Flow
1. **Register**: `POST /api/v1/auth/register`
   ```json
   {
     "username": "newuser",
     "email": "user@example.com",
     "password": "securepassword123"
   }
   ```
2. **Login**: `POST /api/v1/auth/login`
   ```json
   {
     "username": "newuser",
     "password": "securepassword123"
   }
   ```
   Returns access and refresh tokens
3. **Use Token**: Include `Authorization: Bearer <access_token>` header for protected endpoints

### Sample Workflow
1. Create a project: `POST /api/v1/projects`
2. Get your projects: `GET /api/v1/projects/me`
3. Create a task in a project: `POST /api/v1/tasks/{projectId}`
4. Add comments to task: `POST /api/v1/tasks/{taskId}/comments`
5. Update task status: `PUT /api/v1/tasks/status/{taskId}`

## 📦 Dependencies

### Core Spring Boot
- `spring-boot-starter-web`: RESTful web services
- `spring-boot-starter-data-jpa`: ORM and data access
- `spring-boot-starter-validation`: Input validation
- `spring-boot-starter-security`: Authentication and authorization

### Database & Utilities
- `postgresql`: PostgreSQL JDBC driver
- `spring-dotenv`: Environment variable loading
- `lombok`: Reduces boilerplate code
- `jjwt`: JSON Web Token implementation
- `datafaker`: Test data generation (development)
- `springdoc-openapi-starter-webmvc-ui`: OpenAPI 3/Swagger integration

### Testing
- `spring-boot-starter-test`: Testing utilities
- `spring-security-test`: Security testing support

## 🔍 Code Quality & Best Practices

### Implementation Highlights
- **Consistent Logging**: Proper use of SLF4J for debugging and monitoring
- **Null Safety**: Defensive programming practices to prevent NPEs
- **Resource Cleanup**: Proper handling of database connections and transactions
- **Validation Layer**: Validation occurs early in the request lifecycle
- **Error Transparency**: Meaningful error messages without exposing internal details
- **Configuration Externalization**: Environment-specific settings outside code
- **Idempotency Considerations**: Safe retry behavior where applicable
- **Performance Awareness**: Avoiding N+1 queries through proper fetch strategies

### Areas for Future Improvement
- **Caching Layer**: Redis integration for frequently accessed data
- **Asynchronous Processing**: `@Async` for non-blocking operations (notifications, emails)
- **Comprehensive Testing**: Unit and integration test suites
- **Rate Limiting**: Protection against abuse
- **API Versioning**: Strategy for evolving the API without breaking changes
- **Docker Optimization**: Multi-stage builds and health checks
- **Monitoring**: Actuator endpoints and metrics collection
- **Search Functionality**: Full-text search capabilities for tasks/projects
- **File Attachments**: Support for uploading files to tasks/comments

---
