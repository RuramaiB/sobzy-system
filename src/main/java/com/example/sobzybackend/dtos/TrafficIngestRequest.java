package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficIngestRequest {
    private String clientIp;
    private String host;
    private String url;
    private String method;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Integer responseCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String timestamp;
}
