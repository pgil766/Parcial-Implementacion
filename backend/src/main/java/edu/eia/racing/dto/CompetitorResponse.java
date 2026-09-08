package edu.eia.racing.dto;

import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompetitorResponse(
        Long id,
        String name,
        String nickname,
        CompetitorType type,
        LocalDate birthDate,
        Double weight,
        Double height,
        String originCountry,
        CompetitorStatus status,
        LocalDateTime registeredAt,
        int wins,
        int losses,
        int racesCompleted,
        Long teamId) {

    public static CompetitorResponse from(Competitor competitor, Long teamId) {
        return new CompetitorResponse(
                competitor.getId(),
                competitor.getName(),
                competitor.getNickname(),
                competitor.getType(),
                competitor.getBirthDate(),
                competitor.getWeight(),
                competitor.getHeight(),
                competitor.getOriginCountry(),
                competitor.getStatus(),
                competitor.getRegisteredAt(),
                competitor.getWins(),
                competitor.getLosses(),
                competitor.getRacesCompleted(),
                teamId);
    }
}
