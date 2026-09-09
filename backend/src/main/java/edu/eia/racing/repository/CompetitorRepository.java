package edu.eia.racing.repository;

import edu.eia.racing.model.Competitor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CompetitorRepository extends JpaRepository<Competitor, Long>, JpaSpecificationExecutor<Competitor> {

    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Competitor c where c.id = :id")
    Optional<Competitor> findByIdForUpdate(Long id);
}
