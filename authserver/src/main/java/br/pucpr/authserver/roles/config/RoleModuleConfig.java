package br.pucpr.authserver.roles.config;

import br.pucpr.authserver.roles.gateway.RoleGateway;
import br.pucpr.authserver.roles.repository.H2RoleGateway;
import br.pucpr.authserver.roles.repository.RoleRepository;
import br.pucpr.authserver.roles.usecase.InitializeRolesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RoleModuleConfig {
    @Bean
    RoleGateway roleGateway(RoleRepository repository) {
        return new H2RoleGateway(repository);
    }

    @Bean
    InitializeRolesUseCase initializeRolesUseCase(RoleGateway roles) {
        return new InitializeRolesUseCase(roles);
    }
}
