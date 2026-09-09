package edu.eia.racing.repository;

import edu.eia.racing.model.Race;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RaceRepository extends JpaRepository<Race, Long>, JpaSpecificationExecutor<Race> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Race r where r.id = :id")
    Optional<Race> findByIdForUpdate(Long id);
}
