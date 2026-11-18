package br.pucpr.authserver.roles.usecase;

import br.pucpr.authserver.roles.Role;
import br.pucpr.authserver.roles.gateway.RoleGateway;
import org.springframework.transaction.annotation.Transactional;

public class InitializeRolesUseCase {
    private final RoleGateway roles;

    public InitializeRolesUseCase(RoleGateway roles) {
        this.roles = roles;
    }

    @Transactional
    public Role initialize() {
        Role admin = roles.findByName("ADMIN").orElseGet(() -> roles.save(new Role("ADMIN", "Administrador")));
        roles.findByName("PREMIUM").orElseGet(() -> roles.save(new Role("PREMIUM", "Usuário premium")));
        return admin;
    }
}
