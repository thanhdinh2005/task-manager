package com.thanh.taskmanager.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateCommentRequest {

    @NotBlank(message = "Comment content is required")
    private String content;
}