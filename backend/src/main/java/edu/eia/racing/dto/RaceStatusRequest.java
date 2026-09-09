package edu.eia.racing.dto;

import edu.eia.racing.model.enums.RaceStatus;
import jakarta.validation.constraints.NotNull;

public record RaceStatusRequest(
        @NotNull(message = "Status is required") RaceStatus status) {
}
