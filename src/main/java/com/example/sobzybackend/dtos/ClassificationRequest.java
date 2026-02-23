package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRequest {
    private String url;
    private String content;
    private String method;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String deviceId;
}
