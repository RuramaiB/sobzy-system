package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses (login/register) with JWT tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String message;
    private Boolean success;

    // JWT Tokens
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;  // Token expiration time in milliseconds
    private UserResponse user;
}
