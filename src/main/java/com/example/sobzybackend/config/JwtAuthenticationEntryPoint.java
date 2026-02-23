package com.example.sobzybackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * Handles authentication errors and returns appropriate JSON responses
 * Called when an unauthenticated user tries to access a protected resource
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage());

        // Set response type to JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Build error response
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", "Unauthorized access - Invalid or missing authentication token");
        errorDetails.put("error", authException.getMessage());
        errorDetails.put("path", request.getServletPath());
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("statusCode", HttpServletResponse.SC_UNAUTHORIZED);

        // Write JSON response
        response.getOutputStream().write(
                objectMapper.writeValueAsBytes(errorDetails)
        );
    }
}