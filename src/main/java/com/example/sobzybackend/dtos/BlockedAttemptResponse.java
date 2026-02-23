package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedAttemptResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long deviceId;
    private String deviceName;
    private String url;
    private String domain;
    private String category;
    private String reason;
    private String severity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime attemptedAt;
}
