package edu.eia.racing.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrationRejectionRequest(
        @NotBlank(message = "Rejection reason is required") String reason) {
}
