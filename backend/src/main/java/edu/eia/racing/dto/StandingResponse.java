package edu.eia.racing.dto;

public record StandingResponse(
        String participantType,
        Long participantId,
        String participantName,
        int points,
        int wins,
        int losses,
        int racesCompleted) {
}
