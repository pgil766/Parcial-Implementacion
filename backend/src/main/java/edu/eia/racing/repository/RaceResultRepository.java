package edu.eia.racing.repository;

import edu.eia.racing.model.RaceResult;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    List<RaceResult> findByRaceId(Long raceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RaceResult r where r.id = :id")
    Optional<RaceResult> findByIdForUpdate(Long id);

    boolean existsByRegistrationId(Long registrationId);

    boolean existsByRegistrationCompetitorId(Long competitorId);
}
