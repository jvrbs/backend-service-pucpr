package br.pucpr.authserver.users.usecase;

import br.pucpr.authserver.config.AppProperties;
import br.pucpr.authserver.roles.Role;
import br.pucpr.authserver.roles.usecase.InitializeRolesUseCase;
import br.pucpr.authserver.users.User;
import br.pucpr.authserver.users.gateway.UserGateway;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class InitializeAdminUseCase {
    private final InitializeRolesUseCase roles;
    private final UserGateway users;
    private final PasswordEncoder encoder;
    private final AppProperties properties;

    public InitializeAdminUseCase(InitializeRolesUseCase roles, UserGateway users, PasswordEncoder encoder,
            AppProperties properties) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Transactional
    public void initialize() {
        Role admin = roles.initialize();
        var settings = properties.admin();
        User user = users.findByEmail(settings.email()).orElseGet(() ->
                new User(settings.email(), encoder.encode(settings.password()), "Administrador"));
        if (user.getRoles().stream().noneMatch(role -> role.getName().equals("ADMIN"))) {
            user.addRole(admin);
        }
        users.save(user);
    }
}
