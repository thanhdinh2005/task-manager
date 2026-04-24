package com.thanh.taskmanager.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummary {
    private Long id;
    private String fullName;
}