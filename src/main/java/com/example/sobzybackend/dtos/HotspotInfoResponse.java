package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotspotInfoResponse {
    private String ssid;
    private String password;
    private String status;
}
