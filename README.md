# Task Manager API 🚀

A robust, RESTful API for a Task Management application built with Spring Boot. This backend service provides comprehensive features for managing projects, tasks, user authentication, and team collaboration.

## 🛠️ Tech Stack

*   **Framework:** Java / Spring Boot 3.x
*   **Security:** Spring Security with JWT (JSON Web Tokens) & Refresh Tokens
*   **Build Tool:** Maven
*   **Database:** (e.g., PostgreSQL/MySQL - configured via application.yaml)
*   **Containerization:** Docker & Docker Compose

## ✨ Features

*   **Authentication & Authorization:**
    *   User Registration & Login.
    *   Secure endpoints using JWT.
    *   Role-based access control (RBAC).
    *   Change password functionality.
*   **Project Management:**
    *   Create, update, and manage projects.
    *   Add or remove team members from projects.
*   **Task Management:**
    *   Create, assign, update, and filter tasks.
    *   Track task status (e.g., TODO, IN_PROGRESS, DONE) and priorities.
*   ** Task Comments & Collaboration (New):**
    *   Users can add comments to specific tasks to discuss details and updates.
    *   Edit and update existing comments.
    *   Fetch comment history for detailed task tracking.
*   **User Profiles:**
    *   Manage user profiles and summaries.

## 📂 Project Structure

The project follows a standard layered architecture:
```text
backend/src/main/java/com/thanh/taskmanager/
├── config/         # Global configurations (Security, CORS, etc.)
├── controller/     # REST API endpoints (Auth, Project, Task, Comment, Profile)
├── dto/            # Request/Response models (e.g., CreateCommentRequest, UpdateCommentRequest)
├── entity/         # JPA database entities (User, Project, Task, Comment, etc.)
├── exception/      # Global exception handling & custom exceptions
├── mapper/         # Object mapping logic
├── repository/     # Spring Data JPA interfaces
├── security/       # JWT Filters, Custom User Details
├── service/        # Business logic implementation
└── utils/          # Utility and helper classes
