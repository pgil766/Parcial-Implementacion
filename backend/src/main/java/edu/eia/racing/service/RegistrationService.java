package edu.eia.racing.service;

import edu.eia.racing.dto.RegistrationApprovalRequest;
import edu.eia.racing.dto.RegistrationRequest;
import edu.eia.racing.dto.RegistrationResponse;
import edu.eia.racing.dto.RegistrationRejectionRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.Team;
import edu.eia.racing.model.TeamMember;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import edu.eia.racing.model.enums.RegistrationStatus;
import edu.eia.racing.model.enums.TeamStatus;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.TeamRepository;
import edu.eia.racing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RaceRegistrationRepository registrationRepository;
    private final RaceRepository raceRepository;
    private final CompetitorRepository competitorRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public RegistrationResponse create(Long raceId, RegistrationRequest request, String username) {
        Race race = getRaceForUpdate(raceId);
        validateRegistrationWindow(race);
        RegistrationParticipant participant = resolveParticipant(race, request);
        List<RaceRegistration> registrations = registrationRepository.findByRaceId(raceId);
        validateParticipantNotRegistered(registrations, participant, null);
        validateStartingPositionAvailable(registrations, request.startingPosition(), null);

        User user = getUser(username);
        RaceRegistration registration = RaceRegistration.builder()
                .race(race)
                .competitor(participant.competitor())
                .team(participant.team())
                .status(RegistrationStatus.PENDING)
                .startingPosition(request.startingPosition())
                .notes(normalize(request.notes()))
                .registeredBy(user)
                .build();
        try {
            return RegistrationResponse.from(registrationRepository.save(registration));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Participant or starting position is already registered for this race");
        }
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> findByRace(Long raceId) {
        if (!raceRepository.existsById(raceId)) {
            throw new ResourceNotFoundException("Race with ID " + raceId + " was not found");
        }
        return registrationRepository.findByRaceId(raceId).stream().map(RegistrationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationResponse findById(Long id) {
        return RegistrationResponse.from(getRegistration(id));
    }

    @Transactional
    public RegistrationResponse approve(Long id, RegistrationApprovalRequest request) {
        RaceRegistration registration = getRegistrationForUpdate(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new InvalidRequestException("Only pending registrations can be approved");
        }
        Race race = getRaceForUpdate(registration.getRace().getId());
        validateRegistrationWindow(race);
        List<RaceRegistration> registrations = registrationRepository.findByRaceId(race.getId());
        RegistrationRequest currentRequest = new RegistrationRequest(
                registration.getCompetitor() == null ? null : registration.getCompetitor().getId(),
                registration.getTeam() == null ? null : registration.getTeam().getId(),
                registration.getStartingPosition(), registration.getNotes());
        RegistrationParticipant participant = resolveParticipant(race, currentRequest);
        validateParticipantNotRegistered(registrations, participant, id);
        Integer startingPosition = request == null || request.startingPosition() == null
                ? registration.getStartingPosition() : request.startingPosition();
        if (startingPosition == null) {
            throw new InvalidRequestException("Starting position is required before approval");
        }
        validateStartingPositionAvailable(registrations, startingPosition, id);
        long approved = registrations.stream()
                .filter(item -> item.getStatus() == RegistrationStatus.APPROVED)
                .count();
        if (approved >= race.getMaxParticipants()) {
            throw new InvalidRequestException("Race capacity has been reached");
        }
        registration.setStartingPosition(startingPosition);
        registration.setStatus(RegistrationStatus.APPROVED);
        try {
            return RegistrationResponse.from(registrationRepository.save(registration));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Starting position is already assigned in this race");
        }
    }
    @Transactional
    public RegistrationResponse reject(Long id, RegistrationRejectionRequest request) {
        RaceRegistration registration = getRegistrationForUpdate(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new InvalidRequestException("Only pending registrations can be rejected");
        }
        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setStartingPosition(null);
        registration.setNotes(request.reason().trim());
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    @Transactional
    public void delete(Long id) {
        RaceRegistration registration = getRegistrationForUpdate(id);
        if (registration.getStatus() == RegistrationStatus.APPROVED
                && registration.getRace().getStatus() == RaceStatus.IN_PROGRESS) {
            throw new InvalidRequestException("Approved registrations cannot be removed after the race starts");
        }
        registrationRepository.delete(registration);
    }

    private RegistrationParticipant resolveParticipant(Race race, RegistrationRequest request) {
        boolean hasCompetitor = request.competitorId() != null;
        boolean hasTeam = request.teamId() != null;
        if (hasCompetitor == hasTeam) {
            throw new InvalidRequestException("Exactly one competitor or team must be provided");
        }
        if (hasCompetitor) {
            if (race.getType() == RaceType.TEAM) {
                throw new InvalidRequestException("This race only accepts team registrations");
            }
            Competitor competitor = competitorRepository.findByIdForUpdate(request.competitorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Competitor with ID " + request.competitorId()
                            + " was not found"));
            if (competitor.getStatus() != CompetitorStatus.ACTIVE) {
                throw new InvalidRequestException("Only active competitors can be registered");
            }
            return new RegistrationParticipant(competitor, null);
        }
        if (race.getType() == RaceType.INDIVIDUAL) {
            throw new InvalidRequestException("This race only accepts individual registrations");
        }
        Team team = teamRepository.findByIdForUpdate(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team with ID " + request.teamId() + " was not found"));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new InvalidRequestException("Only active teams can be registered");
        }
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndActiveTrue(team.getId());
        if (members.isEmpty() || members.stream().anyMatch(member -> member.getCompetitor().getStatus() != CompetitorStatus.ACTIVE)) {
            throw new InvalidRequestException("A team must have at least one active competitor");
        }
        return new RegistrationParticipant(null, team);
    }

    private void validateParticipantNotRegistered(List<RaceRegistration> registrations,
            RegistrationParticipant participant, Long ignoredRegistrationId) {
        for (RaceRegistration existing : registrations) {
            if (Objects.equals(existing.getId(), ignoredRegistrationId)
                    || existing.getStatus() == RegistrationStatus.REJECTED
                    || existing.getStatus() == RegistrationStatus.CANCELLED) {
                continue;
            }
            if (participant.competitor() != null) {
                if (existing.getCompetitor() != null
                        && existing.getCompetitor().getId().equals(participant.competitor().getId())) {
                    throw new DuplicateResourceException("Competitor is already registered for this race");
                }
                if (existing.getTeam() != null && teamContains(existing.getTeam().getId(), participant.competitor().getId())) {
                    throw new DuplicateResourceException("Competitor is already registered as a team member for this race");
                }
            } else {
                if (existing.getTeam() != null && existing.getTeam().getId().equals(participant.team().getId())) {
                    throw new DuplicateResourceException("Team is already registered for this race");
                }
                List<Long> memberIds = activeMemberIds(participant.team().getId());
                if (existing.getCompetitor() != null && memberIds.contains(existing.getCompetitor().getId())) {
                    throw new DuplicateResourceException("A team member is already registered individually for this race");
                }
                if (existing.getTeam() != null && teamsOverlap(existing.getTeam().getId(), memberIds)) {
                    throw new DuplicateResourceException("A team member is already registered in another team for this race");
                }
            }
        }
    }

    private void validateRegistrationWindow(Race race) {
        if (race.getStatus() != RaceStatus.OPEN_FOR_REGISTRATION) {
            throw new InvalidRequestException("Race is not open for registration");
        }
        if (!LocalDateTime.now().isBefore(race.getRegistrationDeadline())) {
            throw new InvalidRequestException("Registration deadline has passed");
        }
    }

    private void validateStartingPositionAvailable(List<RaceRegistration> registrations, Integer position, Long ignoredId) {
        if (position == null) {
            return;
        }
        boolean occupied = registrations.stream().anyMatch(item -> item.getStartingPosition() != null
                && item.getStartingPosition().equals(position)
                && !Objects.equals(item.getId(), ignoredId)
                && item.getStatus() != RegistrationStatus.REJECTED
                && item.getStatus() != RegistrationStatus.CANCELLED);
        if (occupied) {
            throw new DuplicateResourceException("Starting position is already assigned in this race");
        }
    }

    private boolean teamContains(Long teamId, Long competitorId) {
        return activeMemberIds(teamId).contains(competitorId);
    }

    private boolean teamsOverlap(Long teamId, List<Long> memberIds) {
        return activeMemberIds(teamId).stream().anyMatch(memberIds::contains);
    }


    private List<Long> activeMemberIds(Long teamId) {
        return teamMemberRepository.findByTeamIdAndActiveTrue(teamId).stream()
                .map(member -> member.getCompetitor().getId()).toList();
    }

    private Race getRaceForUpdate(Long id) {
        return raceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race with ID " + id + " was not found"));
    }

    private RaceRegistration getRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration with ID " + id + " was not found"));
    }

    private RaceRegistration getRegistrationForUpdate(Long id) {
        return registrationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration with ID " + id + " was not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " was not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RegistrationParticipant(Competitor competitor, Team team) {
    }
}
