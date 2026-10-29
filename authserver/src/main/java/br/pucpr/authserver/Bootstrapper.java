package br.pucpr.authserver;

import br.pucpr.authserver.config.AppProperties;
import br.pucpr.authserver.roles.Role;
import br.pucpr.authserver.roles.RoleRepository;
import br.pucpr.authserver.users.User;
import br.pucpr.authserver.users.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrapper implements ApplicationRunner {
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties properties;

    public Bootstrapper(RoleRepository roles, UserRepository users, PasswordEncoder encoder, AppProperties properties) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Role admin = roles.findByName("ADMIN").orElseGet(() -> roles.save(new Role("ADMIN", "Administrador")));
        roles.findByName("PREMIUM").orElseGet(() -> roles.save(new Role("PREMIUM", "Usuário premium")));
        User user = users.findByEmail(properties.admin().email()).orElseGet(() ->
                new User(properties.admin().email(), encoder.encode(properties.admin().password()), "Administrador"));
        if (user.getRoles().stream().noneMatch(role -> role.getName().equals("ADMIN"))) {
            user.addRole(admin);
        }
        users.save(user);
    }
}
