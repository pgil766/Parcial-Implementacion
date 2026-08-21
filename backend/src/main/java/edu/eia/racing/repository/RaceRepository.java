package edu.eia.racing.repository;

import edu.eia.racing.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RaceRepository extends JpaRepository<Race, Long>, JpaSpecificationExecutor<Race> {
}
