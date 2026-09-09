package edu.eia.racing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.ResultRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.RaceResult;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.RaceType;
import edu.eia.racing.model.enums.RegistrationStatus;
import edu.eia.racing.model.enums.ResultStatus;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamRepository;
import edu.eia.racing.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock RaceResultRepository resultRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceRegistrationRepository registrationRepository;
    @Mock UserRepository userRepository;
    @Mock CompetitorRepository competitorRepository;
    @Mock TeamRepository teamRepository;
    private ResultService service;
    private Race race;
    private Competitor competitor;
    private RaceRegistration registration;
    private User user;
    private final List<RaceResult> savedResults = new ArrayList<>();

    @BeforeEach
    void setUp() {
        savedResults.clear();
        service = new ResultService(resultRepository, raceRepository, competitorRepository, teamRepository,
                registrationRepository, userRepository);
        race = Race.builder().id(1L).status(RaceStatus.IN_PROGRESS).type(RaceType.INDIVIDUAL).build();
        competitor = Competitor.builder().id(2L).name("Camel").nickname("camel")
                .type(CompetitorType.CAMEL).status(CompetitorStatus.ACTIVE).build();
        registration = RaceRegistration.builder().id(3L).race(race).competitor(competitor)
                .status(RegistrationStatus.APPROVED).build();
        user = User.builder().id(4L).username("organizer")
                .role(Role.builder().name(RoleName.RACE_ORGANIZER).build()).build();
    }

    @Test
    void recordsFinishedResultAndCalculatesPoints() {
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));
        when(resultRepository.findByRaceId(1L)).thenReturn(List.of());
        when(userRepository.findByUsername("organizer")).thenReturn(Optional.of(user));
        when(competitorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(competitor));
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(1L,
                new ResultRequest(3L, 1, 1, 120.0, 2.0, ResultStatus.FINISHED, "winner"), "organizer");

        assertEquals(10, response.points());
        assertEquals(ResultStatus.FINISHED, response.status());
    }

    @Test
    void rejectsResultForRaceThatIsNotInProgress() {
        race.setStatus(RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));

        assertThrows(InvalidRequestException.class, () -> service.create(1L,
                new ResultRequest(3L, 1, 1, 120.0, 0.0, ResultStatus.FINISHED, null), "organizer"));
    }

    @Test
    void rejectsResultForNonApprovedRegistration() {
        registration.setStatus(RegistrationStatus.PENDING);
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));

        assertThrows(InvalidRequestException.class, () -> service.create(1L,
                new ResultRequest(3L, 1, 1, 120.0, 0.0, ResultStatus.FINISHED, null), "organizer"));
    }

    @Test
    void rejectsDuplicatedFinalPosition() {
        RaceResult existing = RaceResult.builder().id(9L).race(race).registration(registration)
                .status(ResultStatus.FINISHED).finalPosition(1).finishTimeSeconds(100.0).build();
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));
        when(resultRepository.findByRaceId(1L)).thenReturn(List.of(existing));

        assertThrows(DuplicateResourceException.class, () -> service.create(1L,
                new ResultRequest(3L, 2, 1, 120.0, 0.0, ResultStatus.FINISHED, null), "organizer"));
    }

    @Test
    void rejectsFinishedResultWithoutPositiveFinishTime() {
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));
        when(resultRepository.findByRaceId(1L)).thenReturn(List.of());

        assertThrows(InvalidRequestException.class, () -> service.create(1L,
                new ResultRequest(3L, 1, 1, null, 0.0, ResultStatus.FINISHED, null), "organizer"));
    }

    @Test
    void rejectsNonFinishedResultWithFinalPosition() {
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));
        when(resultRepository.findByRaceId(1L)).thenReturn(List.of());

        assertThrows(InvalidRequestException.class, () -> service.create(1L,
                new ResultRequest(3L, 1, 1, null, 0.0, ResultStatus.DISQUALIFIED, null), "organizer"));
    }
    @Test
    void updatesPersistedCompetitorStatisticsFromOfficialResults() {
        when(raceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(race));
        when(registrationRepository.findById(3L)).thenReturn(Optional.of(registration));
        when(resultRepository.findByRaceId(1L)).thenReturn(List.of());
        when(userRepository.findByUsername("organizer")).thenReturn(Optional.of(user));
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(invocation -> {
            RaceResult saved = invocation.getArgument(0);
            saved.setId(5L);
            savedResults.add(saved);
            return saved;
        });
        when(competitorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(competitor));
        service.create(1L, new ResultRequest(3L, 1, 1, 120.0, 0.0, ResultStatus.FINISHED, null), "organizer");

        assertEquals(1, competitor.getWins());
        assertEquals(0, competitor.getLosses());
        assertEquals(1, competitor.getRacesCompleted());
    }
}
