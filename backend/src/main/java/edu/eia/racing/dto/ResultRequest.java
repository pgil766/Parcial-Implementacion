package edu.eia.racing.dto;

import edu.eia.racing.model.enums.ResultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ResultRequest(
        @NotNull(message = "Registration is required") @Positive(message = "Registration must be positive") Long registrationId,
        @Positive(message = "Starting position must be positive") Integer startingPosition,
        @Positive(message = "Final position must be positive") Integer finalPosition,
        @Positive(message = "Finish time must be positive") Double finishTimeSeconds,
        @PositiveOrZero(message = "Penalty time cannot be negative") Double penaltyTimeSeconds,
        @NotNull(message = "Result status is required") ResultStatus status,
        String notes) {
}
