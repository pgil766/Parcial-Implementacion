package edu.eia.racing.dto;

import edu.eia.racing.model.Race;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import java.time.LocalDateTime;

public record RaceResponse(
        Long id,
        String name,
        String description,
        LocalDateTime scheduledAt,
        String startLocation,
        String endLocation,
        Double distanceMeters,
        Integer maxParticipants,
        RaceType type,
        RaceStatus status,
        Long organizerId,
        LocalDateTime registrationDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static RaceResponse from(Race race) {
        return new RaceResponse(
                race.getId(), race.getName(), race.getDescription(), race.getScheduledAt(),
                race.getStartLocation(), race.getEndLocation(), race.getDistanceMeters(),
                race.getMaxParticipants(), race.getType(), race.getStatus(), race.getOrganizer().getId(),
                race.getRegistrationDeadline(), race.getCreatedAt(), race.getUpdatedAt());
    }
}
