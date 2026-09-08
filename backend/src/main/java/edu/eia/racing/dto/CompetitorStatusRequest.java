package edu.eia.racing.dto;

import edu.eia.racing.model.enums.CompetitorStatus;
import jakarta.validation.constraints.NotNull;

public record CompetitorStatusRequest(
        @NotNull(message = "Status is required")
        CompetitorStatus status) {
}
