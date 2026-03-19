package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResponse {
    private String decision; // ALLOW, BLOCK
    private String reason;
    private String category;
    private Double confidence;
    private List<String> updatedDenyList;
}
