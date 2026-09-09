package edu.eia.racing.service;

import edu.eia.racing.dto.TeamMemberResponse;
import edu.eia.racing.dto.TeamRequest;
import edu.eia.racing.dto.TeamResponse;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Team;
import edu.eia.racing.model.TeamMember;
import edu.eia.racing.model.enums.TeamStatus;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CompetitorRepository competitorRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;

    @Transactional
    public TeamResponse create(TeamRequest request) {
        Team team = Team.builder()
                .name(normalizeRequired(request.name()))
                .description(normalizeOptional(request.description()))
                .coachName(normalizeOptional(request.coachName()))
                .maxMembers(request.maxMembers())
                .status(request.status() == null ? TeamStatus.ACTIVE : request.status())
                .build();
        return toResponse(saveWithDuplicateHandling(team));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse findById(Long id) {
        return toResponse(getTeam(id));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = getTeam(id);
        if (teamMemberRepository.findByTeamIdAndActiveTrue(id).size() > request.maxMembers()) {
            throw new DuplicateResourceException("Maximum members cannot be lower than current membership");
        }
        team.setName(normalizeRequired(request.name()));
        team.setDescription(normalizeOptional(request.description()));
        team.setCoachName(normalizeOptional(request.coachName()));
        team.setMaxMembers(request.maxMembers());
        if (request.status() != null) {
            team.setStatus(request.status());
        }
        return toResponse(saveWithDuplicateHandling(team));
    }

    @Transactional
    public void delete(Long id) {
        Team team = getTeam(id);
        if (raceRegistrationRepository.existsByTeamId(id)) {
            throw new DuplicateResourceException("Team cannot be deleted because it has official race history");
        }
        if (teamMemberRepository.existsByTeamId(id)) {
            throw new DuplicateResourceException("Team cannot be deleted after membership history exists");
        }
        teamRepository.delete(team);
    }

    @Transactional
    public TeamResponse addMember(Long teamId, Long competitorId) {
        Team team = teamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
        Competitor competitor = competitorRepository.findByIdForUpdate(competitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor not found: " + competitorId));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new DuplicateResourceException("Only active teams can add members");
        }
        if (teamMemberRepository.findByCompetitorIdAndActiveTrue(competitorId).isPresent()) {
            throw new DuplicateResourceException("Competitor already belongs to an active team");
        }
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndActiveTrue(teamId);
        if (activeMembers.size() >= team.getMaxMembers()) {
            throw new DuplicateResourceException("Team has reached its maximum member capacity");
        }
        TeamMember member = teamMemberRepository.findByTeamIdAndCompetitorId(teamId, competitorId)
                .orElseGet(() -> TeamMember.builder().team(team).competitor(competitor).build());
        member.setActive(true);
        member.setLeftAt(null);
        try {
            teamMemberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Competitor is already a member of this team");
        }
        return toResponse(team);
    }

    @Transactional
    public TeamResponse removeMember(Long teamId, Long competitorId) {
        getTeam(teamId);
        TeamMember member = teamMemberRepository.findByTeamIdAndCompetitorId(teamId, competitorId)
                .filter(TeamMember::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active team membership not found"));
        member.setActive(false);
        member.setLeftAt(LocalDateTime.now());
        return toResponse(member.getTeam());
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private Team saveWithDuplicateHandling(Team team) {
        try {
            return teamRepository.save(team);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Team name is already in use");
        }
    }

    private TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamIdAndActiveTrue(team.getId()).stream()
                .map(TeamMemberResponse::from)
                .toList();
        return TeamResponse.from(team, members);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
