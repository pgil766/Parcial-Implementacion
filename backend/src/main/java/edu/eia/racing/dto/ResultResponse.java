package edu.eia.racing.dto;

import edu.eia.racing.model.RaceResult;
import edu.eia.racing.model.enums.ResultStatus;
import java.time.LocalDateTime;

public record ResultResponse(
        Long id,
        Long raceId,
        Long registrationId,
        Long competitorId,
        Long teamId,
        Integer startingPosition,
        Integer finalPosition,
        Double finishTimeSeconds,
        Double penaltyTimeSeconds,
        ResultStatus status,
        String notes,
        Long recordedById,
        LocalDateTime recordedAt,
        int points) {

    public static ResultResponse from(RaceResult result) {
        return new ResultResponse(result.getId(), result.getRace().getId(), result.getRegistration().getId(),
                result.getRegistration().getCompetitor() == null ? null : result.getRegistration().getCompetitor().getId(),
                result.getRegistration().getTeam() == null ? null : result.getRegistration().getTeam().getId(),
                result.getStartingPosition(), result.getFinalPosition(), result.getFinishTimeSeconds(),
                result.getPenaltyTimeSeconds(), result.getStatus(), result.getNotes(), result.getRecordedBy().getId(),
                result.getRecordedAt(), pointsFor(result));
    }

    public static int pointsFor(RaceResult result) {
        if (result.getStatus() != ResultStatus.FINISHED || result.getFinalPosition() == null) {
            return 0;
        }
        return switch (result.getFinalPosition()) {
            case 1 -> 10;
            case 2 -> 7;
            case 3 -> 5;
            case 4 -> 3;
            case 5 -> 1;
            default -> 0;
        };
    }
}
