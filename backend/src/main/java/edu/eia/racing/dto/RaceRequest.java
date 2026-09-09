package edu.eia.racing.dto;

import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record RaceRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotNull(message = "Scheduled time is required") LocalDateTime scheduledAt,
        String startLocation,
        String endLocation,
        @NotNull(message = "Distance is required") @Positive(message = "Distance must be positive")
        Double distanceMeters,
        @NotNull(message = "Maximum participants is required") @Positive(message = "Maximum participants must be positive")
        Integer maxParticipants,
        @NotNull(message = "Race type is required") RaceType type,
        RaceStatus status,
        @NotNull(message = "Organizer is required") Long organizerId,
        @NotNull(message = "Registration deadline is required") LocalDateTime registrationDeadline) {
}
