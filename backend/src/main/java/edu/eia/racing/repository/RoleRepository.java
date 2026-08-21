package edu.eia.racing.repository;

import edu.eia.racing.model.Role;
import edu.eia.racing.model.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
