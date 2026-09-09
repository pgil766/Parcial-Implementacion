package edu.eia.racing.dto;

import jakarta.validation.constraints.Positive;

public record RegistrationApprovalRequest(
        @Positive(message = "Starting position must be positive") Integer startingPosition) {
}
