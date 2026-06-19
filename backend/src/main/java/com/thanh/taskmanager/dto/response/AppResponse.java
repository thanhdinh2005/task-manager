package com.thanh.taskmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // bỏ qua field null trong JSON
public class AppResponse<T> {
    private final int status;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private AppResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> AppResponse<T> success(T data) {
        return new AppResponse<>(200, "Success", data);
    }

    public static <T> AppResponse<T> created(T data) {
        return new AppResponse<>(201, "Created", data);
    }

    public static AppResponse<Void> error(int status, String message) {
        return new AppResponse<>(status, message, null);
    }

    // Overload cho validation errors có data kèm theo
    public static <T> AppResponse<T> error(int status, String message, T data) {
        return new AppResponse<>(status, message, data);
    }
}

