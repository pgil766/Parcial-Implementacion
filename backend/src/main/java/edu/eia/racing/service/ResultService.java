package edu.eia.racing.service;

import edu.eia.racing.dto.ResultRequest;
import edu.eia.racing.dto.ResultResponse;
import edu.eia.racing.dto.StandingResponse;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidRequestException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Race;
import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.RaceResult;
import edu.eia.racing.model.Team;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.RaceStatus;
import edu.eia.racing.model.enums.ResultStatus;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamRepository;
import edu.eia.racing.repository.UserRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final RaceResultRepository resultRepository;
    private final RaceRepository raceRepository;
    private final CompetitorRepository competitorRepository;
    private final TeamRepository teamRepository;

    private final RaceRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResultResponse create(Long raceId, ResultRequest request, String username) {
        Race race = getRaceForUpdate(raceId);
        validateRaceInProgress(race);
        RaceRegistration registration = getApprovedRegistration(request.registrationId(), raceId);
        if (resultRepository.findByRaceId(raceId).stream()
                .anyMatch(result -> result.getRegistration().getId().equals(registration.getId()))) {
            throw new DuplicateResourceException("A result already exists for this registration");
        }
        List<RaceResult> results = resultRepository.findByRaceId(raceId);
        validateResult(request, results, null);
        RaceResult result = buildResult(race, registration, request, getUser(username));
        RaceResult saved;
        try {
            saved = resultRepository.save(result);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Result conflicts with an existing official result");
        }
        applyStatistics(saved, 1);
        return ResultResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> findByRace(Long raceId) {
        if (!raceRepository.existsById(raceId)) {
            throw new ResourceNotFoundException("Race with ID " + raceId + " was not found");
        }
        return resultRepository.findByRaceId(raceId).stream().map(ResultResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResultResponse findById(Long id) {
        return ResultResponse.from(getResult(id));
    }

    @Transactional
    public ResultResponse update(Long id, ResultRequest request, String username) {
        RaceResult result = resultRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result with ID " + id + " was not found"));
        Race race = getRaceForUpdate(result.getRace().getId());
        validateRaceInProgress(race);
        RaceRegistration registration = getApprovedRegistration(request.registrationId(), race.getId());
        if (!registration.getId().equals(result.getRegistration().getId())) {
            throw new InvalidRequestException("A result cannot be reassigned to another registration");
        }
        List<RaceResult> results = resultRepository.findByRaceId(race.getId());
        validateResult(request, results, id);
        ResultStatus previousStatus = result.getStatus();
        Integer previousFinalPosition = result.getFinalPosition();
        result.setStartingPosition(request.startingPosition());
        result.setFinalPosition(request.finalPosition());
        result.setFinishTimeSeconds(request.finishTimeSeconds());
        result.setPenaltyTimeSeconds(request.penaltyTimeSeconds() == null ? 0.0 : request.penaltyTimeSeconds());
        result.setStatus(request.status());
        result.setNotes(normalize(request.notes()));
        RaceResult saved = resultRepository.save(result);
        applyStatisticsDelta(saved, previousStatus, previousFinalPosition);
        return ResultResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StandingResponse> standings() {
        return standingsFor(resultRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<StandingResponse> competitorStandings() {
        return standingsFor(resultRepository.findAll()).stream()
                .filter(item -> item.participantType().equals("COMPETITOR")).toList();
    }

    @Transactional(readOnly = true)
    public List<StandingResponse> teamStandings() {
        return standingsFor(resultRepository.findAll()).stream()
                .filter(item -> item.participantType().equals("TEAM")).toList();
    }

    private List<StandingResponse> standingsFor(List<RaceResult> results) {
        Map<String, StandingTotals> totals = new HashMap<>();
        for (RaceResult result : results) {
            RaceRegistration registration = result.getRegistration();
            String type;
            Long participantId;
            String name;
            if (registration.getCompetitor() != null) {
                Competitor competitor = registration.getCompetitor();
                type = "COMPETITOR";
                participantId = competitor.getId();
                name = competitor.getName();
            } else {
                Team team = registration.getTeam();
                type = "TEAM";
                participantId = team.getId();
                name = team.getName();
            }
            String key = type + ":" + participantId;
            StandingTotals current = totals.computeIfAbsent(key,
                    ignored -> new StandingTotals(type, participantId, name));
            current.points += ResultResponse.pointsFor(result);
            if (result.getStatus() == ResultStatus.FINISHED) {
                current.racesCompleted++;
                if (Integer.valueOf(1).equals(result.getFinalPosition())) {
                    current.wins++;
                } else {
                    current.losses++;
                }
            }
        }
        return totals.values().stream()
                .sorted(Comparator.comparingInt((StandingTotals item) -> item.points).reversed()
                        .thenComparing(StandingTotals::name))
                .map(StandingTotals::toResponse).toList();
    }

    private void applyStatistics(RaceResult result, int direction) {
        if (result.getStatus() != ResultStatus.FINISHED) {
            return;
        }
        boolean winner = Integer.valueOf(1).equals(result.getFinalPosition());
        if (result.getRegistration().getCompetitor() != null) {
            Competitor competitor = competitorRepository.findByIdForUpdate(result.getRegistration().getCompetitor().getId())
                    .orElseThrow();
            competitor.setRacesCompleted(competitor.getRacesCompleted() + direction);
            if (winner) {
                competitor.setWins(competitor.getWins() + direction);
            } else {
                competitor.setLosses(competitor.getLosses() + direction);
            }
            competitorRepository.save(competitor);
        } else if (result.getRegistration().getTeam() != null) {
            Team team = teamRepository.findByIdForUpdate(result.getRegistration().getTeam().getId())
                    .orElseThrow();
            if (winner) {
                team.setWins(team.getWins() + direction);
            } else {
                team.setLosses(team.getLosses() + direction);
            }
            teamRepository.save(team);
        }
    }

    private void applyStatisticsDelta(RaceResult result, ResultStatus previousStatus, Integer previousFinalPosition) {
        if (previousStatus == ResultStatus.FINISHED) {
            RaceResult previous = RaceResult.builder().registration(result.getRegistration())
                    .status(previousStatus).finalPosition(previousFinalPosition).build();
            applyStatistics(previous, -1);
        }
        applyStatistics(result, 1);
    }

    private void validateResult(ResultRequest request, List<RaceResult> results, Long ignoredId) {
        if (request.status() == ResultStatus.FINISHED) {
            if (request.finalPosition() == null || request.finishTimeSeconds() == null) {
                throw new InvalidRequestException("Finished results require final position and finish time");
            }
        } else if (request.finalPosition() != null || request.finishTimeSeconds() != null) {
            throw new InvalidRequestException("Non-finished results cannot have final position or finish time");
        }
        if (request.status() == ResultStatus.DISQUALIFIED && Integer.valueOf(1).equals(request.finalPosition())) {
            throw new InvalidRequestException("A disqualified participant cannot win");
        }
        if (request.status() == ResultStatus.FINISHED && request.finalPosition() != null) {
            boolean duplicatePosition = results.stream().anyMatch(result -> !result.getId().equals(ignoredId)
                    && result.getStatus() == ResultStatus.FINISHED
                    && request.finalPosition().equals(result.getFinalPosition()));
            if (duplicatePosition) {
                throw new DuplicateResourceException("Final positions cannot be duplicated");
            }
        }
    }

    private RaceResult buildResult(Race race, RaceRegistration registration, ResultRequest request, User user) {
        return RaceResult.builder().race(race).registration(registration).startingPosition(request.startingPosition())
                .finalPosition(request.finalPosition()).finishTimeSeconds(request.finishTimeSeconds())
                .penaltyTimeSeconds(request.penaltyTimeSeconds() == null ? 0.0 : request.penaltyTimeSeconds())
                .status(request.status()).notes(normalize(request.notes())).recordedBy(user).build();
    }

    private RaceRegistration getApprovedRegistration(Long registrationId, Long raceId) {
        RaceRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration with ID " + registrationId + " was not found"));
        if (!registration.getRace().getId().equals(raceId)) {
            throw new InvalidRequestException("Registration does not belong to this race");
        }
        if (registration.getStatus() != edu.eia.racing.model.enums.RegistrationStatus.APPROVED) {
            throw new InvalidRequestException("Only approved registrations can receive results");
        }
        return registration;
    }

    private void validateRaceInProgress(Race race) {
        if (race.getStatus() != RaceStatus.IN_PROGRESS) {
            throw new InvalidRequestException("Results can only be recorded while the race is in progress");
        }
    }

    private Race getRaceForUpdate(Long id) {
        return raceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race with ID " + id + " was not found"));
    }

    private RaceResult getResult(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result with ID " + id + " was not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " was not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class StandingTotals {
        private final String type;
        private final Long id;
        private final String name;
        private int points;
        private int wins;
        private int losses;
        private int racesCompleted;

        private StandingTotals(String type, Long id, String name) {
            this.type = type;
            this.id = id;
            this.name = name;
        }

        private String name() {
            return name;
        }

        private StandingResponse toResponse() {
            return new StandingResponse(type, id, name, points, wins, losses, racesCompleted);
        }
    }
}
