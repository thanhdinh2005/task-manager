package com.thanh.taskmanager.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
}
