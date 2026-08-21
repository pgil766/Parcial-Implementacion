package edu.eia.racing.repository;

import edu.eia.racing.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);
}
