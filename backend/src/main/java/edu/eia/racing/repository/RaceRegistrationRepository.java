package edu.eia.racing.repository;

import edu.eia.racing.model.RaceRegistration;
import edu.eia.racing.model.enums.RegistrationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RaceRegistrationRepository extends JpaRepository<RaceRegistration, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RaceRegistration r where r.id = :id")
    Optional<RaceRegistration> findByIdForUpdate(Long id);

    List<RaceRegistration> findByRaceId(Long raceId);
    List<RaceRegistration> findByTeamId(Long teamId);
    boolean existsByRaceIdAndCompetitorIdAndStatusIn(Long raceId, Long competitorId,
            Collection<RegistrationStatus> statuses);
    boolean existsByCompetitorId(Long competitorId);
    boolean existsByTeamId(Long teamId);
}
