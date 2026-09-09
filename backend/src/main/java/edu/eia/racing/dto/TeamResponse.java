package edu.eia.racing.dto;

import edu.eia.racing.model.Team;
import edu.eia.racing.model.enums.TeamStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        TeamStatus status,
        String coachName,
        int wins,
        int losses,
        int maxMembers,
        List<TeamMemberResponse> members) {

    public static TeamResponse from(Team team, List<TeamMemberResponse> members) {
        return new TeamResponse(
                team.getId(), team.getName(), team.getDescription(), team.getCreatedAt(),
                team.getStatus(), team.getCoachName(), team.getWins(), team.getLosses(),
                team.getMaxMembers(), List.copyOf(members));
    }
}
