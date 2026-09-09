package edu.eia.racing.repository;

import edu.eia.racing.model.TeamMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamIdAndActiveTrue(Long teamId);

    boolean existsByTeamId(Long teamId);

    Optional<TeamMember> findByCompetitorIdAndActiveTrue(Long competitorId);
    List<TeamMember> findByCompetitorIdInAndActiveTrue(List<Long> competitorIds);

    Optional<TeamMember> findByTeamIdAndCompetitorId(Long teamId, Long competitorId);

    boolean existsByTeamIdAndCompetitorId(Long teamId, Long competitorId);
    boolean existsByCompetitorId(Long competitorId);
}
