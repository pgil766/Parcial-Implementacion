package edu.eia.racing.dto;

import edu.eia.racing.model.enums.CompetitorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CompetitorRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Nickname is required")
        String nickname,
        @NotNull(message = "Competitor type is required")
        CompetitorType type,
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
        @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be positive")
        Double weight,
        @NotNull(message = "Height is required")
        @Positive(message = "Height must be positive")
        Double height,
        String originCountry) {
}
