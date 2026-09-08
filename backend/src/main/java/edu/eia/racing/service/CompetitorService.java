package edu.eia.racing.service;

import edu.eia.racing.dto.CompetitorRequest;
import edu.eia.racing.dto.CompetitorResponse;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompetitorService {

    private final CompetitorRepository competitorRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final RaceResultRepository raceResultRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public CompetitorResponse create(CompetitorRequest request) {
        String nickname = normalizedNickname(request.nickname());
        if (competitorRepository.existsByNickname(nickname)) {
            throw new DuplicateResourceException("Nickname is already in use");
        }

        Competitor competitor = Competitor.builder()
                .name(request.name().trim())
                .nickname(nickname)
                .type(request.type())
                .birthDate(request.birthDate())
                .weight(request.weight())
                .height(request.height())
                .originCountry(normalizeOptional(request.originCountry()))
                .status(CompetitorStatus.ACTIVE)
                .build();
        return CompetitorResponse.from(saveWithDuplicateHandling(competitor), null);
    }

    @Transactional(readOnly = true)
    public Page<CompetitorResponse> findAll(String name, String nickname, CompetitorType type,
            CompetitorStatus status, Pageable pageable) {
        Specification<Competitor> specification = (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (name != null && !name.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (nickname != null && !nickname.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nickname")),
                        "%" + nickname.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Competitor> page = competitorRepository.findAll(specification, pageable);
        List<Long> competitorIds = page.getContent().stream().map(Competitor::getId).toList();
        Map<Long, Long> teamIds = new HashMap<>();
        if (!competitorIds.isEmpty()) {
            teamMemberRepository.findByCompetitorIdInAndActiveTrue(competitorIds)
                    .forEach(member -> teamIds.put(member.getCompetitor().getId(), member.getTeam().getId()));
        }
        return page.map(competitor -> CompetitorResponse.from(competitor, teamIds.get(competitor.getId())));
    }

    @Transactional(readOnly = true)
    public CompetitorResponse findById(Long id) {
        return toResponse(getCompetitor(id));
    }

    @Transactional
    public CompetitorResponse update(Long id, CompetitorRequest request) {
        Competitor competitor = getCompetitor(id);
        String nickname = normalizedNickname(request.nickname());
        if (competitorRepository.existsByNicknameAndIdNot(nickname, id)) {
            throw new DuplicateResourceException("Nickname is already in use");
        }

        competitor.setName(request.name().trim());
        competitor.setNickname(nickname);
        competitor.setType(request.type());
        competitor.setBirthDate(request.birthDate());
        competitor.setWeight(request.weight());
        competitor.setHeight(request.height());
        competitor.setOriginCountry(normalizeOptional(request.originCountry()));
        return toResponse(saveWithDuplicateHandling(competitor));
    }

    @Transactional
    public CompetitorResponse updateStatus(Long id, CompetitorStatus status) {
        Competitor competitor = getCompetitor(id);
        competitor.setStatus(status);
        return toResponse(competitorRepository.save(competitor));
    }

    @Transactional
    public void delete(Long id) {
        Competitor competitor = getCompetitor(id);
        if (raceResultRepository.existsByRegistrationCompetitorId(id)
                || raceRegistrationRepository.existsByCompetitorId(id)
                || teamMemberRepository.existsByCompetitorId(id)) {
            throw new DuplicateResourceException(
                    "Competitor with related records cannot be physically deleted; retire it instead");
        }
        competitorRepository.delete(competitor);
    }

    private Competitor getCompetitor(Long id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor with ID " + id + " was not found"));
    }
    private Competitor saveWithDuplicateHandling(Competitor competitor) {
        try {
            return competitorRepository.save(competitor);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Nickname is already in use");
        }
    }

    private CompetitorResponse toResponse(Competitor competitor) {
        Long teamId = teamMemberRepository.findByCompetitorIdAndActiveTrue(competitor.getId())
                .map(member -> member.getTeam().getId())
                .orElse(null);
        return CompetitorResponse.from(competitor, teamId);
    }

    private String normalizedNickname(String nickname) {
        return nickname.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
