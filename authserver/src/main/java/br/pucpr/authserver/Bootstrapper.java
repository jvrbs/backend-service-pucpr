package br.pucpr.authserver;

import br.pucpr.authserver.users.usecase.InitializeAdminUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Bootstrapper implements ApplicationRunner {
    private final InitializeAdminUseCase admin;

    public Bootstrapper(InitializeAdminUseCase admin) {
        this.admin = admin;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        admin.initialize();
    }
}
