package com.thanh.taskmanager.mapper;

import com.thanh.taskmanager.dto.response.UserResponse;
import com.thanh.taskmanager.dto.response.UserSummary;
import com.thanh.taskmanager.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
