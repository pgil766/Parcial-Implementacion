package edu.eia.racing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.RaceRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.User;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RaceServiceTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceRegistrationRepository raceRegistrationRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;

    private RaceService service;

    @BeforeEach
    void setUp() {
        service = new RaceService(raceRepository, raceRegistrationRepository, raceResultRepository,
                teamMemberRepository, userRepository, auditLogService);
    }

    @Test
    void createRejectsRaceScheduledInThePast() {
        RaceRequest request = request(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusHours(1));

        assertThrows(InvalidRequestException.class, () -> service.create(request));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createRejectsDeadlineAfterScheduledTime() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2);
        RaceRequest request = request(scheduledAt, scheduledAt.plusMinutes(1));

        assertThrows(InvalidRequestException.class, () -> service.create(request));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createDefaultsToDraftAndResolvesOrganizer() {
        User organizer = User.builder().id(7L)
                .role(Role.builder().name(RoleName.RACE_ORGANIZER).build())
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(organizer));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1)));

        assertEquals(RaceStatus.DRAFT, response.status());
        assertEquals(7L, response.organizerId());
    }

    @Test
    void statusTransitionRejectsSkippingRegistrationStates() {
        Race race = race(RaceStatus.DRAFT);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));

        assertThrows(DuplicateResourceException.class,
                () -> service.updateStatus(1L, RaceStatus.IN_PROGRESS));
        verify(raceRepository, never()).save(any());
    }

    @Test
    void startingRaceRequiresTwoApprovedParticipants() {
        Race race = race(RaceStatus.CLOSED_FOR_REGISTRATION);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.findByRaceId(1L)).thenReturn(List.of());

        assertThrows(DuplicateResourceException.class,
                () -> service.updateStatus(1L, RaceStatus.IN_PROGRESS));
    }

    @Test
    void completedRaceCannotBeEdited() {
        Race race = race(RaceStatus.COMPLETED);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));

        assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, request(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1))));
    }

    @Test
    void completedRaceRequiresOfficialResults() {
        Race race = race(RaceStatus.IN_PROGRESS);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(raceResultRepository.findByRaceId(1L)).thenReturn(List.of());

        assertThrows(DuplicateResourceException.class,
                () -> service.updateStatus(1L, RaceStatus.COMPLETED));
    }

    @Test
    void updateRejectsCapacityBelowApprovedParticipants() {
        Race race = race(RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.findByRaceId(1L)).thenReturn(List.of(
                RaceRegistration.builder().status(RegistrationStatus.APPROVED).build(),
                RaceRegistration.builder().status(RegistrationStatus.APPROVED).build()));

        RaceRequest request = new RaceRequest("Race", "Description", race.getScheduledAt(), "Start", "Finish",
                1000.0, 1, RaceType.INDIVIDUAL, null, 7L, race.getRegistrationDeadline());

        assertThrows(DuplicateResourceException.class, () -> service.update(1L, request));
        verify(raceRepository, never()).save(any());
    }

    @Test
    void updateRejectsViewerAsOrganizer() {
        Race race = race(RaceStatus.DRAFT);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.findByRaceId(1L)).thenReturn(List.of());
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().id(7L)
                .role(Role.builder().name(RoleName.VIEWER).build()).build()));

        assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, request(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1))));
    }

    @Test
    void updateChangesRaceName() {
        Race race = race(RaceStatus.DRAFT);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.findByRaceId(1L)).thenReturn(List.of());
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().id(7L)
                .role(Role.builder().name(RoleName.RACE_ORGANIZER).build()).build()));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(1L, new RaceRequest("Updated race", "Description",
                race.getScheduledAt(), "Start", "Finish", 1000.0, 10, RaceType.INDIVIDUAL,
                null, 7L, race.getRegistrationDeadline()));

        assertEquals("Updated race", response.name());
    }

    private RaceRequest request(LocalDateTime scheduledAt, LocalDateTime deadline) {
        return new RaceRequest("Race", "Description", scheduledAt, "Start", "Finish", 1000.0,
                10, RaceType.INDIVIDUAL, null, 7L, deadline);
    }

    private Race race(RaceStatus status) {
        return Race.builder()
                .id(1L)
                .name("Race")
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .registrationDeadline(LocalDateTime.now().plusDays(1))
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(RaceType.INDIVIDUAL)
                .status(status)
                .organizer(User.builder().id(7L).build())
                .build();
    }
}
