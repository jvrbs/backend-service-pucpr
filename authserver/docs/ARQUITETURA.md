# Arquitetura do AuthServer

O projeto é um monólito modular com um único `pom.xml`. Os módulos de aplicação são `users`, `roles` e `football`. Segurança, tratamento de erros e configuração geral são compartilhados.

| Módulo | Configuração Spring | Casos de uso |
| --- | --- | --- |
| Usuários | `UserModuleConfig` | Cadastro, login, consultas e inicialização do administrador |
| Papéis | `RoleModuleConfig` | Inicialização dos papéis ADMIN e PREMIUM |
| Futebol | `FootballModuleConfig` | Operações de ligas, times, jogadores e títulos. Também configura a carga inicial no perfil de demonstração. |

Os controllers chamam `usecase`. Os casos de uso dependem dos contratos em `gateway`. Cada `ModuleConfig` instancia os casos de uso e liga os contratos aos adaptadores `H2*Gateway`, que vivem em `repository` junto às interfaces Spring Data JPA. Os adaptadores concentram a paginação, as consultas e a tradução de conflitos do H2. Os casos de uso delimitam as transações e atribuem o significado de negócio aos conflitos.

As entidades JPA são compartilhadas entre os casos de uso e os adaptadores. A inicialização do administrador chama o caso de uso de papéis na mesma transação. A carga de futebol acessa os adaptadores pelos gateways.

O [guia de futebol](FUTEBOL.md) lista os endpoints, permissões e exemplos.

## Árvore de arquivos

A árvore inclui os arquivos do projeto presentes na área de trabalho e exclui metadados Git, arquivos ignorados e a saída de compilação em `target/`.

```text
backend-service-pucpr/
├── authserver/
│   ├── .mvn/
│   │   └── wrapper/
│   │       └── maven-wrapper.properties
│   ├── docs/
│   │   ├── ARQUITETURA.md
│   │   ├── DEMO.md
│   │   ├── ENTREGA.md
│   │   └── FUTEBOL.md
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── br/
│   │   │   │       └── pucpr/
│   │   │   │           └── authserver/
│   │   │   │               ├── config/
│   │   │   │               │   ├── AppConfig.java
│   │   │   │               │   ├── AppProperties.java
│   │   │   │               │   ├── OpenApiConfig.java
│   │   │   │               │   └── SecurityConfig.java
│   │   │   │               ├── exceptions/
│   │   │   │               │   ├── ApiException.java
│   │   │   │               │   ├── ApiExceptionHandler.java
│   │   │   │               │   ├── ApiProblems.java
│   │   │   │               │   └── PersistenceConflictException.java
│   │   │   │               ├── football/
│   │   │   │               │   ├── config/
│   │   │   │               │   │   └── FootballModuleConfig.java
│   │   │   │               │   ├── gateway/
│   │   │   │               │   │   ├── ChampionshipGateway.java
│   │   │   │               │   │   ├── LeagueGateway.java
│   │   │   │               │   │   ├── PlayerGateway.java
│   │   │   │               │   │   └── TeamGateway.java
│   │   │   │               │   ├── repository/
│   │   │   │               │   │   ├── ChampionshipRepository.java
│   │   │   │               │   │   ├── H2ChampionshipGateway.java
│   │   │   │               │   │   ├── H2LeagueGateway.java
│   │   │   │               │   │   ├── H2PlayerGateway.java
│   │   │   │               │   │   ├── H2TeamGateway.java
│   │   │   │               │   │   ├── LeagueRepository.java
│   │   │   │               │   │   ├── PlayerRepository.java
│   │   │   │               │   │   └── TeamRepository.java
│   │   │   │               │   ├── usecase/
│   │   │   │               │   │   ├── LeagueUseCase.java
│   │   │   │               │   │   ├── PlayerUseCase.java
│   │   │   │               │   │   └── TeamUseCase.java
│   │   │   │               │   ├── Championship.java
│   │   │   │               │   ├── FootballDtos.java
│   │   │   │               │   ├── FootballSeed.java
│   │   │   │               │   ├── League.java
│   │   │   │               │   ├── LeagueController.java
│   │   │   │               │   ├── Player.java
│   │   │   │               │   ├── PlayerController.java
│   │   │   │               │   ├── Team.java
│   │   │   │               │   └── TeamController.java
│   │   │   │               ├── roles/
│   │   │   │               │   ├── config/
│   │   │   │               │   │   └── RoleModuleConfig.java
│   │   │   │               │   ├── gateway/
│   │   │   │               │   │   └── RoleGateway.java
│   │   │   │               │   ├── repository/
│   │   │   │               │   │   ├── H2RoleGateway.java
│   │   │   │               │   │   └── RoleRepository.java
│   │   │   │               │   ├── usecase/
│   │   │   │               │   │   └── InitializeRolesUseCase.java
│   │   │   │               │   └── Role.java
│   │   │   │               ├── security/
│   │   │   │               │   ├── AuthUser.java
│   │   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │   │               │   ├── JwtService.java
│   │   │   │               │   ├── ProblemAccessDeniedHandler.java
│   │   │   │               │   ├── ProblemAuthenticationEntryPoint.java
│   │   │   │               │   └── ProblemWriter.java
│   │   │   │               ├── users/
│   │   │   │               │   ├── config/
│   │   │   │               │   │   └── UserModuleConfig.java
│   │   │   │               │   ├── dto/
│   │   │   │               │   │   ├── CreateUserRequest.java
│   │   │   │               │   │   ├── LoginRequest.java
│   │   │   │               │   │   ├── LoginResponse.java
│   │   │   │               │   │   └── UserResponse.java
│   │   │   │               │   ├── gateway/
│   │   │   │               │   │   └── UserGateway.java
│   │   │   │               │   ├── repository/
│   │   │   │               │   │   ├── H2UserGateway.java
│   │   │   │               │   │   └── UserRepository.java
│   │   │   │               │   ├── usecase/
│   │   │   │               │   │   ├── InitializeAdminUseCase.java
│   │   │   │               │   │   └── UserUseCase.java
│   │   │   │               │   ├── validation/
│   │   │   │               │   │   ├── PasswordPolicy.java
│   │   │   │               │   │   ├── PasswordValidator.java
│   │   │   │               │   │   └── ValidPassword.java
│   │   │   │               │   ├── Emails.java
│   │   │   │               │   ├── User.java
│   │   │   │               │   └── UserController.java
│   │   │   │               ├── AuthserverApplication.java
│   │   │   │               ├── Bootstrapper.java
│   │   │   │               └── RootController.java
│   │   │   └── resources/
│   │   │       ├── application-dev.yaml
│   │   │       ├── application-prod.yaml
│   │   │       ├── application.yaml
│   │   │       └── football-demo.json
│   │   └── test/
│   │       ├── java/
│   │       │   └── br/
│   │       │       └── pucpr/
│   │       │           └── authserver/
│   │       │               ├── config/
│   │       │               │   └── AppPropertiesTests.java
│   │       │               ├── users/
│   │       │               │   └── usecase/
│   │       │               │       └── UserUseCaseTests.java
│   │       │               └── AuthserverApplicationTests.java
│   │       └── resources/
│   │           └── application-test.yaml
│   ├── .gitattributes
│   ├── .gitignore
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── README.md
└── README.md
```
