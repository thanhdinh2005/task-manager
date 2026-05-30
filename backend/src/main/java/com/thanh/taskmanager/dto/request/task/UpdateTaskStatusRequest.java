package com.thanh.taskmanager.dto.request.task;

import com.thanh.taskmanager.entity.enums.TodoStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TodoStatus status;
}