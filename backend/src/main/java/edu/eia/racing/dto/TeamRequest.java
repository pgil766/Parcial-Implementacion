package edu.eia.racing.dto;

import edu.eia.racing.model.enums.TeamStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        String coachName,
        @NotNull(message = "Maximum members is required")
        @Positive(message = "Maximum members must be positive") Integer maxMembers,
        TeamStatus status) {
}
