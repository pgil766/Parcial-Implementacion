package edu.eia.racing.service;

import edu.eia.racing.dto.RaceRequest;
import edu.eia.racing.dto.RaceResponse;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.TeamMember;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import edu.eia.racing.model.enums.RegistrationStatus;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RaceService {

    private static final Set<RaceStatus> TERMINAL_STATUSES = EnumSet.of(RaceStatus.COMPLETED, RaceStatus.CANCELLED);

    private final RaceRepository raceRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceResultRepository raceResultRepository;
    private final TeamMemberRepository teamMemberRepository;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public RaceResponse create(RaceRequest request) {
        validateCreateDates(request);
        if (request.status() != null && request.status() != RaceStatus.DRAFT) {
            throw new DuplicateResourceException("New races must start in DRAFT status");
        }
        User organizer = getUser(request.organizerId());
        Race race = Race.builder()
                .name(request.name().trim())
                .description(normalizeOptional(request.description()))
                .scheduledAt(request.scheduledAt())
                .startLocation(normalizeOptional(request.startLocation()))
                .endLocation(normalizeOptional(request.endLocation()))
                .distanceMeters(request.distanceMeters())
                .maxParticipants(request.maxParticipants())
                .type(request.type())
                .status(request.status() == null ? RaceStatus.DRAFT : request.status())
                .organizer(organizer)
                .registrationDeadline(request.registrationDeadline())
                .build();
        return RaceResponse.from(saveWithDuplicateHandling(race));
    }

    @Transactional(readOnly = true)
    public List<RaceResponse> findAll() {
        return raceRepository.findAll().stream().map(RaceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RaceResponse findById(Long id) {
        return RaceResponse.from(getRace(id));
    }

    @Transactional
    public RaceResponse update(Long id, RaceRequest request) {
        Race race = raceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + id));
        if (race.getStatus() == RaceStatus.COMPLETED) {
            throw new DuplicateResourceException("Completed races cannot be edited");
        }
        validateUpdateDates(race, request);
        validateCapacityChange(race, request.maxParticipants());
        validateTypeChange(race, request.type());
        race.setName(request.name().trim());
        race.setDescription(normalizeOptional(request.description()));
        race.setScheduledAt(request.scheduledAt());
        race.setStartLocation(normalizeOptional(request.startLocation()));
        race.setEndLocation(normalizeOptional(request.endLocation()));
        race.setDistanceMeters(request.distanceMeters());
        race.setMaxParticipants(request.maxParticipants());
        race.setType(request.type());
        race.setRegistrationDeadline(request.registrationDeadline());
        race.setOrganizer(getUser(request.organizerId()));
        if (request.status() != null && request.status() != race.getStatus()) {
            applyTransition(race, request.status());
            if (request.status() == RaceStatus.CANCELLED) {
                auditLogService.recordCurrentUser("RACE_CANCELLED", "Race", race.getId(), "Race cancelled");
            }
        }
        return RaceResponse.from(saveWithDuplicateHandling(race));
    }

    @Transactional
    public RaceResponse updateStatus(Long id, RaceStatus status) {
        Race race = raceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + id));
        applyTransition(race, status);
        Race saved = raceRepository.save(race);
        if (status == RaceStatus.CANCELLED) {
            auditLogService.recordCurrentUser("RACE_CANCELLED", "Race", saved.getId(), "Race cancelled");
        }
        return RaceResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Race race = raceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + id));
        if (race.getStatus() == RaceStatus.COMPLETED || !raceRegistrationRepository.findByRaceId(id).isEmpty()
                || !raceResultRepository.findByRaceId(id).isEmpty()) {
            throw new DuplicateResourceException("Race cannot be deleted after official history exists");
        }
        raceRepository.delete(race);
    }

    private void applyTransition(Race race, RaceStatus target) {
        if (target == null) {
            throw new DuplicateResourceException("Race status is required");
        }
        RaceStatus current = race.getStatus();
        if (current == target) {
            return;
        }
        if (TERMINAL_STATUSES.contains(current)) {
            throw new DuplicateResourceException("Race status cannot change after it is " + current);
        }
        boolean valid = target == RaceStatus.CANCELLED
                || current == RaceStatus.DRAFT && target == RaceStatus.OPEN_FOR_REGISTRATION
                || current == RaceStatus.OPEN_FOR_REGISTRATION && target == RaceStatus.CLOSED_FOR_REGISTRATION
                || current == RaceStatus.CLOSED_FOR_REGISTRATION && target == RaceStatus.IN_PROGRESS
                || current == RaceStatus.IN_PROGRESS && target == RaceStatus.COMPLETED;
        if (!valid) {
            throw new DuplicateResourceException("Invalid race status transition: " + current + " -> " + target);
        }
        if (target == RaceStatus.IN_PROGRESS) {
            List<RaceRegistration> approvedRegistrations = raceRegistrationRepository.findByRaceId(race.getId()).stream()
                    .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                    .toList();
            validateApprovedEligibility(approvedRegistrations);
            if (approvedRegistrations.size() < 2) {
                throw new DuplicateResourceException("At least two approved participants are required to start");
            }
            if (approvedRegistrations.size() > race.getMaxParticipants()) {
                throw new DuplicateResourceException("Race capacity cannot be exceeded");
            }
        }
        if (target == RaceStatus.COMPLETED && raceResultRepository.findByRaceId(race.getId()).isEmpty()) {
            throw new DuplicateResourceException("Race cannot be completed without official results");
        }
        race.setStatus(target);
    }

    private void validateApprovedEligibility(List<RaceRegistration> registrations) {
        for (RaceRegistration registration : registrations) {
            if (registration.getCompetitor() != null) {
                if (registration.getCompetitor().getStatus() != CompetitorStatus.ACTIVE) {
                    throw new InvalidRequestException("All approved competitors must remain active before the race starts");
                }
                continue;
            }
            if (registration.getTeam() == null
                    || registration.getTeam().getStatus() != edu.eia.racing.model.enums.TeamStatus.ACTIVE) {
                throw new InvalidRequestException("All approved teams must remain active before the race starts");
            }
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndActiveTrue(registration.getTeam().getId());
            if (members.isEmpty()
                    || members.stream().anyMatch(member -> member.getCompetitor().getStatus() != CompetitorStatus.ACTIVE)) {
                throw new InvalidRequestException("All approved teams must have active competitors before the race starts");
            }
        }
    }

    private void validateCreateDates(RaceRequest request) {
        if (request.scheduledAt() == null || !request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Scheduled time must be in the future");
        }
        validateDeadline(request);
    }

    private void validateUpdateDates(Race race, RaceRequest request) {
        if (request.scheduledAt() == null) {
            throw new InvalidRequestException("Scheduled time is required");
        }
        if (!request.scheduledAt().equals(race.getScheduledAt())
                && !request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("A changed scheduled time must be in the future");
        }
        validateDeadline(request);
    }

    private void validateDeadline(RaceRequest request) {
        if (request.registrationDeadline() == null || !request.registrationDeadline().isBefore(request.scheduledAt())) {
            throw new InvalidRequestException("Registration deadline must be before the scheduled time");
        }
    }

    private void validateCapacityChange(Race race, Integer proposedCapacity) {
        long approved = raceRegistrationRepository.findByRaceId(race.getId()).stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                .count();
        if (proposedCapacity < approved) {
            throw new DuplicateResourceException("Maximum participants cannot be lower than approved participants");
        }
    }

    private void validateTypeChange(Race race, RaceType proposedType) {
        if (proposedType == race.getType()) {
            return;
        }
        boolean incompatible = raceRegistrationRepository.findByRaceId(race.getId()).stream()
                .anyMatch(registration -> proposedType == RaceType.INDIVIDUAL && registration.getTeam() != null
                        || proposedType == RaceType.TEAM && registration.getCompetitor() != null);
        if (incompatible) {
            throw new DuplicateResourceException("Race type is incompatible with existing registrations");
        }
    }

    private Race getRace(Long id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + id));
    }

    private User getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found: " + id));
        if (user.getRole() == null || (user.getRole().getName() != RoleName.ADMIN
                && user.getRole().getName() != RoleName.RACE_ORGANIZER)) {
            throw new DuplicateResourceException("Race organizer must have an ADMIN or RACE_ORGANIZER role");
        }
        return user;
    }


    private Race saveWithDuplicateHandling(Race race) {
        try {
            return raceRepository.save(race);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Race could not be saved because of a data conflict");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
