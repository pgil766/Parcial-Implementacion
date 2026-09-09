package edu.eia.racing.controller;

import edu.eia.racing.dto.CompetitorRequest;
import edu.eia.racing.dto.CompetitorResponse;
import edu.eia.racing.dto.CompetitorStatusRequest;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.service.CompetitorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/competitors")
@Validated
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorService competitorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompetitorResponse> create(@Valid @RequestBody CompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitorService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<CompetitorResponse>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) CompetitorType type,
            @RequestParam(required = false) CompetitorStatus status,
            @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(competitorService.findAll(name, nickname, type, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(competitorService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompetitorResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody CompetitorRequest request) {
        return ResponseEntity.ok(competitorService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompetitorResponse> updateStatus(@PathVariable @Positive Long id,
            @Valid @RequestBody CompetitorStatusRequest request) {
        return ResponseEntity.ok(competitorService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        competitorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
