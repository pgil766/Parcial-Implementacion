package edu.eia.racing.repository;

import edu.eia.racing.model.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompetitorRepository extends JpaRepository<Competitor, Long>, JpaSpecificationExecutor<Competitor> {

    boolean existsByNickname(String nickname);
}
