package com.thanh.taskmanager.dto.request.task;

import com.thanh.taskmanager.entity.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class UpdateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    private Priority priority;

    private Long assigneeId;        // null = unassign

    private LocalDate dueDate;      // null = xóa deadline
}
