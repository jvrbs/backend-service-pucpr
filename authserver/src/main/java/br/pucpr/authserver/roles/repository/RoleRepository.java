package br.pucpr.authserver.roles.repository;

import br.pucpr.authserver.roles.Role;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
