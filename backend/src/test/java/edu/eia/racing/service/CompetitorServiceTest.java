package edu.eia.racing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.CompetitorRequest;
import org.springframework.dao.DataIntegrityViolationException;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.enums.CompetitorStatus;
import edu.eia.racing.model.enums.CompetitorType;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.RaceResultRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompetitorServiceTest {

    @Mock
    private CompetitorRepository competitorRepository;
    @Mock
    private RaceRegistrationRepository raceRegistrationRepository;
    @Mock
    private RaceResultRepository raceResultRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private CompetitorService competitorService;

    @Test
    void createBuildsAnActiveCompetitorAndNormalizesNickname() {
        CompetitorRequest request = request(" Byte ", "  CamelByte ");
        when(competitorRepository.existsByNickname("camelbyte")).thenReturn(false);
        when(competitorRepository.save(any(Competitor.class))).thenAnswer(invocation -> {
            Competitor competitor = invocation.getArgument(0);
            competitor.setId(10L);
            return competitor;
        });

        var response = competitorService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Byte");
        assertThat(response.nickname()).isEqualTo("camelbyte");
        assertThat(response.status()).isEqualTo(CompetitorStatus.ACTIVE);
    }

    @Test
    void createMapsConcurrentNicknameConstraintToConflict() {
        when(competitorRepository.existsByNickname("byte")).thenReturn(false);
        when(competitorRepository.save(any(Competitor.class)))
                .thenThrow(new DataIntegrityViolationException("unique nickname"));

        assertThatThrownBy(() -> competitorService.create(request("Byte", "Byte")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Nickname is already in use");
    }

    @Test
    void createRejectsDuplicateNickname() {
        when(competitorRepository.existsByNickname("byte")).thenReturn(true);

        assertThatThrownBy(() -> competitorService.create(request("Byte", "Byte")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Nickname is already in use");
        verify(competitorRepository, never()).save(any());
    }

    @Test
    void updateRejectsNicknameOwnedByAnotherCompetitor() {
        when(competitorRepository.findById(4L)).thenReturn(Optional.of(competitor(4L)));
        when(competitorRepository.existsByNicknameAndIdNot("byte", 4L)).thenReturn(true);

        assertThatThrownBy(() -> competitorService.update(4L, request("Byte", "Byte")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(competitorRepository, never()).save(any());
    }

    @Test
    void updatePreservesCurrentStatus() {
        Competitor competitor = competitor(4L);
        competitor.setStatus(CompetitorStatus.INJURED);
        when(competitorRepository.findById(4L)).thenReturn(Optional.of(competitor));
        when(competitorRepository.existsByNicknameAndIdNot("byte", 4L)).thenReturn(false);
        when(competitorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = competitorService.update(4L, request("New Byte", "Byte"));

        assertThat(response.status()).isEqualTo(CompetitorStatus.INJURED);
        assertThat(response.name()).isEqualTo("New Byte");
    }

    @Test
    void deleteRejectsCompetitorWithOfficialResults() {
        when(competitorRepository.findById(4L)).thenReturn(Optional.of(competitor(4L)));
        when(raceResultRepository.existsByRegistrationCompetitorId(4L)).thenReturn(true);

        assertThatThrownBy(() -> competitorService.delete(4L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("related records");
        verify(competitorRepository, never()).delete(any(Competitor.class));
    }

    @Test
    void deleteRejectsCompetitorWithTeamMembership() {
        when(competitorRepository.findById(4L)).thenReturn(Optional.of(competitor(4L)));
        when(teamMemberRepository.existsByCompetitorId(4L)).thenReturn(true);

        assertThatThrownBy(() -> competitorService.delete(4L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("related records");
        verify(competitorRepository, never()).delete(any(Competitor.class));
    }

    @Test
    void missingCompetitorReturnsNotFound() {
        when(competitorRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> competitorService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competitor with ID 404 was not found");
    }

    private CompetitorRequest request(String name, String nickname) {
        return new CompetitorRequest(name, nickname, CompetitorType.CAMEL,
                null, 500.0, 200.0, "Colombia");
    }

    private Competitor competitor(Long id) {
        return Competitor.builder()
                .id(id)
                .name("Byte")
                .nickname("byte")
                .type(CompetitorType.CAMEL)
                .weight(500.0)
                .height(200.0)
                .status(CompetitorStatus.ACTIVE)
                .build();
    }
}
