package edu.eia.racing.repository;

import edu.eia.racing.model.RaceResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    List<RaceResult> findByRaceId(Long raceId);
}
