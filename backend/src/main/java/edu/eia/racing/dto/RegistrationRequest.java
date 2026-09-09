package edu.eia.racing.dto;

import jakarta.validation.constraints.Positive;

public record RegistrationRequest(
        Long competitorId,
        Long teamId,
        @Positive(message = "Starting position must be positive") Integer startingPosition,
        String notes) {
}
