package com.thanh.taskmanager.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private UserSummary createdBy;
    private int memberCount;
    private int taskCount;
    private LocalDateTime createdAt;
}