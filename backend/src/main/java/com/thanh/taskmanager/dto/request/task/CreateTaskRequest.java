package com.thanh.taskmanager.dto.request.task;

import com.thanh.taskmanager.entity.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;     // optional

    private Priority priority;      // optional, default MEDIUM trong Service

    private Long assigneeId;        // optional, phải là thành viên project

    private LocalDate dueDate;      // optional
}