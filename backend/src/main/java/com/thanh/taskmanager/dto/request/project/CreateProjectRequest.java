package com.thanh.taskmanager.dto.request.project;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    private String name;

    private String description;
}