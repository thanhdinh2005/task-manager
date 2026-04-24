package com.thanh.taskmanager.dto.response;

import com.thanh.taskmanager.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private LocalDateTime joinedAt;
}