package edu.eia.racing.controller;

import edu.eia.racing.dto.RaceRequest;
import edu.eia.racing.dto.RaceResponse;
import edu.eia.racing.dto.RaceStatusRequest;
import edu.eia.racing.service.RaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/races")
@Validated
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RaceResponse> create(@Valid @RequestBody RaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raceService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RaceResponse>> findAll() {
        return ResponseEntity.ok(raceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaceResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(raceService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RaceResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody RaceRequest request) {
        return ResponseEntity.ok(raceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RaceResponse> updateStatus(@PathVariable @Positive Long id,
            @Valid @RequestBody RaceStatusRequest request) {
        return ResponseEntity.ok(raceService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        raceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
