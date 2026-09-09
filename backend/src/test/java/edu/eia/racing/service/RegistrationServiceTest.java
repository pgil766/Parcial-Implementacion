package edu.eia.racing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.RegistrationApprovalRequest;
import edu.eia.racing.dto.RegistrationRequest;
import edu.eia.racing.dto.RegistrationRejectionRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import edu.eia.racing.model.enums.RegistrationStatus;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.TeamRepository;
import edu.eia.racing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock RaceRegistrationRepository registrationRepository;
    @Mock RaceRepository raceRepository;
    @Mock CompetitorRepository competitorRepository;
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock UserRepository userRepository;
    @Mock RaceResultRepository raceResultRepository;

    private RegistrationService service;
    private Race race;
    private Competitor competitor;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(registrationRepository, raceRepository, competitorRepository,
                teamRepository, teamMemberRepository, userRepository, raceResultRepository);
        race = Race.builder().id(10L).type(RaceType.INDIVIDUAL).status(RaceStatus.OPEN_FOR_REGISTRATION)
                .registrationDeadline(LocalDateTime.now().plusHours(1)).maxParticipants(3).build();
        competitor = Competitor.builder().id(20L).name("Byte").nickname("byte")
                .type(CompetitorType.CAMEL).status(CompetitorStatus.ACTIVE).build();
        user = User.builder().id(30L).username("organizer").role(Role.builder().name(RoleName.RACE_ORGANIZER).build())
                .build();
    }

    @Test
    void registersActiveCompetitor() {
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));
        when(registrationRepository.findByRaceId(10L)).thenReturn(List.of());
        when(userRepository.findByUsername("organizer")).thenReturn(Optional.of(user));
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(invocation -> {
            RaceRegistration saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.setRegisteredAt(LocalDateTime.now());
            return saved;
        });

        var response = service.create(10L, new RegistrationRequest(20L, null, 1, null), "organizer");

        assertEquals(RegistrationStatus.PENDING, response.status());
        assertEquals(20L, response.competitorId());
        verify(registrationRepository).save(any(RaceRegistration.class));
    }

    @Test
    void rejectsSuspendedCompetitor() {
        competitor.setStatus(CompetitorStatus.SUSPENDED);
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));

        assertThrows(InvalidRequestException.class,
                () -> service.create(10L, new RegistrationRequest(20L, null, 1, null), "organizer"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateCompetitorRegistration() {
        RaceRegistration existing = RaceRegistration.builder().id(60L).race(race).competitor(competitor)
                .status(RegistrationStatus.PENDING).build();
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));
        when(registrationRepository.findByRaceId(10L)).thenReturn(List.of(existing));

        assertThrows(DuplicateResourceException.class,
                () -> service.create(10L, new RegistrationRequest(20L, null, 1, null), "organizer"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void rejectsRegistrationAfterDeadline() {
        race.setRegistrationDeadline(LocalDateTime.now().minusMinutes(1));
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));

        assertThrows(InvalidRequestException.class,
                () -> service.create(10L, new RegistrationRequest(20L, null, 1, null), "organizer"));
        verify(competitorRepository, never()).findById(any());
    }

    @Test
    void rejectsCompetitorForTeamRace() {
        race.setType(RaceType.TEAM);
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));

        assertThrows(InvalidRequestException.class,
                () -> service.create(10L, new RegistrationRequest(20L, null, 1, null), "organizer"));
        verify(competitorRepository, never()).findById(any());
    }

    @Test
    void approvalRequiresUniqueStartingPosition() {
        RaceRegistration pending = RaceRegistration.builder().id(70L).race(race).competitor(competitor)
                .status(RegistrationStatus.PENDING).startingPosition(1).build();
        RaceRegistration occupied = RaceRegistration.builder().id(71L).race(race).competitor(
                Competitor.builder().id(21L).build()).status(RegistrationStatus.APPROVED).startingPosition(2).build();
        when(registrationRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(pending));
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));
        when(registrationRepository.findByRaceId(10L)).thenReturn(List.of(pending, occupied));

        assertThrows(DuplicateResourceException.class,
                () -> service.approve(70L, new RegistrationApprovalRequest(2)));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void approvalRequiresPositionWhenNoneWasAssigned() {
        RaceRegistration pending = RaceRegistration.builder().id(70L).race(race).competitor(competitor)
                .status(RegistrationStatus.PENDING).build();
        when(registrationRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(pending));
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));
        when(registrationRepository.findByRaceId(10L)).thenReturn(List.of(pending));

        assertThrows(InvalidRequestException.class, () -> service.approve(70L, null));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void approvalRechecksCurrentCompetitorEligibility() {
        competitor.setStatus(CompetitorStatus.SUSPENDED);
        RaceRegistration pending = RaceRegistration.builder().id(70L).race(race).competitor(competitor)
                .status(RegistrationStatus.PENDING).startingPosition(1).build();
        when(registrationRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(pending));
        when(raceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(race));
        when(competitorRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(competitor));

        assertThrows(InvalidRequestException.class,
                () -> service.approve(70L, new RegistrationApprovalRequest(1)));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void rejectionStoresReasonAndClearsPosition() {
        RaceRegistration pending = RaceRegistration.builder().id(70L).race(race).competitor(competitor)
                .registeredBy(user).status(RegistrationStatus.PENDING).startingPosition(1).build();
        when(registrationRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(pending));
        when(registrationRepository.save(pending)).thenReturn(pending);
        var response = service.reject(70L, new RegistrationRejectionRequest("Competitor is unavailable"));

        assertEquals(RegistrationStatus.REJECTED, response.status());
        assertEquals("Competitor is unavailable", response.notes());
        assertEquals(null, response.startingPosition());
    }
    @Test
    void deleteRejectsRegistrationWithOfficialResult() {
        RaceRegistration approved = RaceRegistration.builder().id(70L).race(race).competitor(competitor)
                .status(RegistrationStatus.APPROVED).build();
        when(registrationRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(approved));
        when(raceResultRepository.existsByRegistrationId(70L)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> service.delete(70L));
        verify(registrationRepository, never()).delete(any());
    }
}
