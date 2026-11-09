package br.pucpr.authserver.users.config;

import br.pucpr.authserver.config.AppProperties;
import br.pucpr.authserver.roles.usecase.InitializeRolesUseCase;
import br.pucpr.authserver.security.JwtService;
import br.pucpr.authserver.users.gateway.UserGateway;
import br.pucpr.authserver.users.repository.H2UserGateway;
import br.pucpr.authserver.users.repository.UserRepository;
import br.pucpr.authserver.users.usecase.InitializeAdminUseCase;
import br.pucpr.authserver.users.usecase.UserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class UserModuleConfig {
    @Bean
    UserGateway userGateway(UserRepository repository) {
        return new H2UserGateway(repository);
    }

    @Bean
    UserUseCase userUseCase(UserGateway users, PasswordEncoder encoder, JwtService jwtService) {
        return new UserUseCase(users, encoder, jwtService);
    }

    @Bean
    InitializeAdminUseCase initializeAdminUseCase(InitializeRolesUseCase roles, UserGateway users,
            PasswordEncoder encoder, AppProperties properties) {
        return new InitializeAdminUseCase(roles, users, encoder, properties);
    }
}
