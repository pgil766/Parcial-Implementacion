package edu.eia.racing.dto;

import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.enums.RegistrationStatus;
import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long raceId,
        Long competitorId,
        Long teamId,
        LocalDateTime registeredAt,
        RegistrationStatus status,
        Integer startingPosition,
        String notes,
        Long registeredById) {

    public static RegistrationResponse from(RaceRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getRace().getId(),
                registration.getCompetitor() == null ? null : registration.getCompetitor().getId(),
                registration.getTeam() == null ? null : registration.getTeam().getId(),
                registration.getRegisteredAt(),
                registration.getStatus(),
                registration.getStartingPosition(),
                registration.getNotes(),
                registration.getRegisteredBy().getId());
    }
}
