package edu.eia.racing.repository;

import edu.eia.racing.model.RaceRegistration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceRegistrationRepository extends JpaRepository<RaceRegistration, Long> {

    List<RaceRegistration> findByRaceId(Long raceId);
    boolean existsByCompetitorId(Long competitorId);
}
