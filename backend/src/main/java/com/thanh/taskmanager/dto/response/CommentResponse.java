package com.thanh.taskmanager.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummary author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean edited;
}