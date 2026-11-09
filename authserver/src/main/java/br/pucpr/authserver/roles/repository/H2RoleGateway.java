package br.pucpr.authserver.roles.repository;

import br.pucpr.authserver.roles.gateway.RoleGateway;
import br.pucpr.authserver.roles.Role;
import java.util.Optional;

public class H2RoleGateway implements RoleGateway {
    private final RoleRepository repository;

    public H2RoleGateway(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Role> findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public Role save(Role role) {
        return repository.save(role);
    }
}
