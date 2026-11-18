package br.pucpr.authserver.roles.gateway;

import br.pucpr.authserver.roles.Role;
import java.util.Optional;

public interface RoleGateway {
    Optional<Role> findByName(String name);
    Role save(Role role);
}
