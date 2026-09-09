package edu.eia.racing.dto;

import jakarta.validation.constraints.Positive;

public record RegistrationRequest(
        @Positive(message = "Competitor ID must be positive") Long competitorId,
        @Positive(message = "Team ID must be positive") Long teamId,
        @Positive(message = "Starting position must be positive") Integer startingPosition,
        String notes) {
}
