package edu.eia.racing.controller;

import edu.eia.racing.dto.ResultRequest;
import edu.eia.racing.dto.ResultResponse;
import edu.eia.racing.dto.StandingResponse;
import edu.eia.racing.service.ResultService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @PostMapping("/races/{raceId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<ResultResponse> create(@PathVariable @Positive Long raceId,
            @Valid @RequestBody ResultRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resultService.create(raceId, request, authentication.getName()));
    }

    @GetMapping("/races/{raceId}/results")
    public ResponseEntity<List<ResultResponse>> findByRace(@PathVariable @Positive Long raceId) {
        return ResponseEntity.ok(resultService.findByRace(raceId));
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<ResultResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(resultService.findById(id));
    }

    @PutMapping("/results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<ResultResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody ResultRequest request, Authentication authentication) {
        return ResponseEntity.ok(resultService.update(id, request, authentication.getName()));
    }

    @GetMapping("/standings")
    public ResponseEntity<List<StandingResponse>> standings() {
        return ResponseEntity.ok(resultService.standings());
    }

    @GetMapping("/standings/competitors")
    public ResponseEntity<List<StandingResponse>> competitorStandings() {
        return ResponseEntity.ok(resultService.competitorStandings());
    }

    @GetMapping("/standings/teams")
    public ResponseEntity<List<StandingResponse>> teamStandings() {
        return ResponseEntity.ok(resultService.teamStandings());
    }
}
