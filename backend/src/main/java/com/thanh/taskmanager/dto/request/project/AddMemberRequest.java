package com.thanh.taskmanager.dto.request.project;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
