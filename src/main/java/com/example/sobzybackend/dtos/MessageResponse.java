package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic message response DTO for success/error messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    private String message;
    private Boolean success;
    private Integer statusCode;
    private LocalDateTime timestamp;
    private Object data;

    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }

    public MessageResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static MessageResponse success(String message) {
        return MessageResponse.builder()
                .message(message)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static MessageResponse success(String message, Object data) {
        return MessageResponse.builder()
                .message(message)
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static MessageResponse error(String message) {
        return MessageResponse.builder()
                .message(message)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static MessageResponse error(String message, Integer statusCode) {
        return MessageResponse.builder()
                .message(message)
                .success(false)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
