package edu.eia.racing.dto;

import edu.eia.racing.model.TeamMember;
import java.time.LocalDateTime;

public record TeamMemberResponse(
        Long competitorId,
        String name,
        String nickname,
        LocalDateTime joinedAt) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getCompetitor().getId(),
                member.getCompetitor().getName(),
                member.getCompetitor().getNickname(),
                member.getJoinedAt());
    }
}
