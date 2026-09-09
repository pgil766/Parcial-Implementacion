package edu.eia.racing.controller;

import edu.eia.racing.dto.RegistrationApprovalRequest;
import edu.eia.racing.dto.RegistrationRequest;
import edu.eia.racing.dto.RegistrationResponse;
import edu.eia.racing.dto.RegistrationRejectionRequest;
import edu.eia.racing.service.RegistrationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/races/{raceId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RegistrationResponse> create(@PathVariable @Positive Long raceId,
            @Valid @RequestBody RegistrationRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.create(raceId, request, authentication.getName()));
    }

    @GetMapping("/races/{raceId}/registrations")
    public ResponseEntity<List<RegistrationResponse>> findByRace(@PathVariable @Positive Long raceId) {
        return ResponseEntity.ok(registrationService.findByRace(raceId));
    }

    @GetMapping("/registrations/{id}")
    public ResponseEntity<RegistrationResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(registrationService.findById(id));
    }

    @PatchMapping("/registrations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RegistrationResponse> approve(@PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) RegistrationApprovalRequest request) {
        return ResponseEntity.ok(registrationService.approve(id, request));
    }

    @PatchMapping("/registrations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<RegistrationResponse> reject(@PathVariable @Positive Long id,
            @Valid @RequestBody RegistrationRejectionRequest request) {
        return ResponseEntity.ok(registrationService.reject(id, request));
    }

    @DeleteMapping("/registrations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        registrationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
