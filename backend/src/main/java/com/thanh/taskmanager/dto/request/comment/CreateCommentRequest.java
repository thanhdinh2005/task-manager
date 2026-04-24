package com.thanh.taskmanager.dto.request.comment;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

    @NotBlank(message = "Comment content is required")
    private String content;
}
