package com.thanh.taskmanager.dto.request.project;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
