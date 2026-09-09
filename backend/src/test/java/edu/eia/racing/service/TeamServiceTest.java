package edu.eia.racing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.TeamRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.model.Competitor;
import edu.eia.racing.model.Team;
import edu.eia.racing.model.TeamMember;
import edu.eia.racing.model.enums.TeamStatus;
import edu.eia.racing.repository.CompetitorRepository;
import edu.eia.racing.repository.RaceRegistrationRepository;
import edu.eia.racing.repository.TeamMemberRepository;
import edu.eia.racing.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock CompetitorRepository competitorRepository;
    @Mock RaceRegistrationRepository raceRegistrationRepository;

    private TeamService service;

    @BeforeEach
    void setUp() {
        service = new TeamService(teamRepository, teamMemberRepository, competitorRepository,
                raceRegistrationRepository);
    }

    @Test
    void createDefaultsToActiveAndNormalizesFields() {
        Team saved = team(1L, "Racing Team");
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team value = invocation.getArgument(0);
            value.setId(1L);
            return value;
        });
        when(teamMemberRepository.findByTeamIdAndActiveTrue(1L)).thenReturn(List.of());

        var response = service.create(new TeamRequest(" Racing Team ", " Desc ", " Coach ", 3, null));

        assertEquals("Racing Team", response.name());
        assertEquals(TeamStatus.ACTIVE, response.status());
        assertEquals(3, response.maxMembers());
    }

    @Test
    void addMemberRejectsCompetitorAlreadyInAnotherActiveTeam() {
        Team team = team(1L, "One");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(competitorRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(Competitor.builder().id(2L).build()));
        when(teamMemberRepository.findByCompetitorIdAndActiveTrue(2L))
                .thenReturn(Optional.of(TeamMember.builder().build()));

        assertThrows(DuplicateResourceException.class, () -> service.addMember(1L, 2L));
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void addMemberRejectsWhenCapacityIsReached() {
        Team team = team(1L, "One");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(competitorRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(Competitor.builder().id(2L).build()));
        when(teamMemberRepository.findByCompetitorIdAndActiveTrue(2L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findByTeamIdAndActiveTrue(1L)).thenReturn(List.of(
                TeamMember.builder().build(), TeamMember.builder().build()));

        assertThrows(DuplicateResourceException.class, () -> service.addMember(1L, 2L));
    }

    @Test
    void removeMemberMarksHistoricalMembershipInactive() {
        Team team = team(1L, "One");
        TeamMember member = TeamMember.builder().team(team).active(true).build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndCompetitorId(1L, 2L)).thenReturn(Optional.of(member));
        when(teamMemberRepository.findByTeamIdAndActiveTrue(1L)).thenReturn(List.of());

        service.removeMember(1L, 2L);

        assertEquals(false, member.isActive());
        verify(teamMemberRepository).findByTeamIdAndActiveTrue(1L);
    }

    @Test
    void updateRejectsCapacityBelowCurrentMembership() {
        Team team = team(1L, "One");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndActiveTrue(1L)).thenReturn(List.of(
                TeamMember.builder().build(), TeamMember.builder().build()));

        assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, new TeamRequest("One", null, null, 1, null)));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void deleteRejectsTeamWithRaceHistory() {
        Team team = team(1L, "One");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(raceRegistrationRepository.existsByTeamId(1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.delete(1L));
        verify(teamRepository, never()).delete(any());
    }

    private Team team(Long id, String name) {
        return Team.builder().id(id).name(name).status(TeamStatus.ACTIVE).maxMembers(2).build();
    }
}
