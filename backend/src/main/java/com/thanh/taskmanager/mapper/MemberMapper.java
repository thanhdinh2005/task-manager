package com.thanh.taskmanager.mapper;

import com.thanh.taskmanager.dto.response.MemberResponse;
import com.thanh.taskmanager.entity.ProjectMember;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {
    public MemberResponse toResponse(ProjectMember member) {
        return MemberResponse.builder()
                .userId(member.getUser().getId())
                .fullName(member.getUser().getFullName())
                .email(member.getUser().getEmail())
                .role(member.getRole())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}
