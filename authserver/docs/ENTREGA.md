# Relatório da primeira entrega parcial

Validação realizada em 29/10/2026, com Microsoft OpenJDK 25.0.3, Maven Wrapper 3.10.0 e Spring Boot 4.0.9.

## Resultado real

- Comando: `.\mvnw.cmd -B -ntp clean verify`, dentro de `authserver/` (equivalente Windows de `./mvnw clean verify`).
- **BUILD SUCCESS**, em 47,537 segundos.
- **48 testes**, zero falhas, zero erros e zero ignorados.
- 44 testes de aplicação/integração, 2 de configuração e 2 de serviço/tratamento de erros.
- 30 classes de produção e 3 classes de teste compiladas com `release 25`.
- JAR executável gerado: `authserver/target/authserver-0.0.1-SNAPSHOT.jar`.
- Relatórios Maven: `authserver/target/surefire-reports/`.
- `git diff --check` sem erros de whitespace.

## Verificação HTTP do JAR

O JAR foi iniciado com perfil dev, porta local dinâmica e credenciais temporárias geradas em memória. O processo foi encerrado ao terminar. Nenhum token foi impresso.

| Verificação | HTTP observado |
| --- | --- |
| Health público | 200 |
| Cadastro com persistência | 201 |
| Cadastro sem campos | 422 |
| Consulta sem token | 401 |
| Login e geração de JWT | 200 |
| Listagem com JWT | 200 |
| Consulta do usuário cadastrado | 200 |
| OpenAPI | 200 |
| Swagger UI | 200 |
| E-mail duplicado | 409 |

A especificação OpenAPI foi testada para exatamente cinco operações de negócio. As respostas de consulta e cadastro não contêm senha/hash. O JAR não contém `application-test.yaml`, JUnit nem Mockito. Nenhuma credencial real foi adicionada aos arquivos entregues; os valores fixos de configuração existem apenas nos recursos de teste.

## Dependências e compatibilidade

Adicionadas `jjwt-api`, `jjwt-impl` e `jjwt-jackson` 0.13.0, e `springdoc-openapi-starter-webmvc-ui` 3.0.3. JUnit foi fixado em 5.14.4 para preservar a stack exigida. Mockito e os starters MVC, Security, Validation, JPA e H2 continuam gerenciados pelo Boot 4.0.9.

O módulo de console H2 foi removido do POM por não fazer parte desta entrega. O banco H2 permanece. O arquivo inicial `application.properties` foi substituído pelos perfis YAML solicitados.

As soluções para SpringExtension/JUnit 5, coexistência de Jackson, Maven Wrapper Windows e sockets do JDK estão detalhadas no [README](../README.md#compatibilidade-de-bibliotecas). O Mockito é carregado como agente explícito apenas nos testes.

## Limites e execução

H2 é em memória e perde os dados ao encerrar. Não há endpoints da segunda etapa, gerenciamento de papéis, atualização/exclusão, paginação, refresh ou revogação. Não restaram bloqueios de compilação ou testes.

Para executar, selecione JDK 25, configure `APP_SECURITY_SECRET`, `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`, e use `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'`. Consulte os comandos completos no [README](../README.md).

O [roteiro da apresentação](DEMO.md) demonstra os sete passos: health, cadastro, erro 422, login, erro 401, listagem com JWT e consulta por ID.

Não foi realizado commit, push ou alteração remota no GitHub. Os caminhos abaixo são relativos à raiz do repositório.

## Arquivos criados

- `authserver/README.md`
- `authserver/docs/DEMO.md`
- `authserver/docs/ENTREGA.md`
- `authserver/src/main/java/br/pucpr/authserver/Bootstrapper.java`
- `authserver/src/main/java/br/pucpr/authserver/RootController.java`
- `authserver/src/main/java/br/pucpr/authserver/config/AppConfig.java`
- `authserver/src/main/java/br/pucpr/authserver/config/AppProperties.java`
- `authserver/src/main/java/br/pucpr/authserver/config/OpenApiConfig.java`
- `authserver/src/main/java/br/pucpr/authserver/config/SecurityConfig.java`
- `authserver/src/main/java/br/pucpr/authserver/exceptions/ApiException.java`
- `authserver/src/main/java/br/pucpr/authserver/exceptions/ApiExceptionHandler.java`
- `authserver/src/main/java/br/pucpr/authserver/exceptions/ApiProblems.java`
- `authserver/src/main/java/br/pucpr/authserver/roles/Role.java`
- `authserver/src/main/java/br/pucpr/authserver/roles/RoleRepository.java`
- `authserver/src/main/java/br/pucpr/authserver/security/AuthUser.java`
- `authserver/src/main/java/br/pucpr/authserver/security/JwtAuthenticationFilter.java`
- `authserver/src/main/java/br/pucpr/authserver/security/JwtService.java`
- `authserver/src/main/java/br/pucpr/authserver/security/ProblemAccessDeniedHandler.java`
- `authserver/src/main/java/br/pucpr/authserver/security/ProblemAuthenticationEntryPoint.java`
- `authserver/src/main/java/br/pucpr/authserver/security/ProblemWriter.java`
- `authserver/src/main/java/br/pucpr/authserver/users/Emails.java`
- `authserver/src/main/java/br/pucpr/authserver/users/User.java`
- `authserver/src/main/java/br/pucpr/authserver/users/UserController.java`
- `authserver/src/main/java/br/pucpr/authserver/users/UserRepository.java`
- `authserver/src/main/java/br/pucpr/authserver/users/UserService.java`
- `authserver/src/main/java/br/pucpr/authserver/users/dto/CreateUserRequest.java`
- `authserver/src/main/java/br/pucpr/authserver/users/dto/LoginRequest.java`
- `authserver/src/main/java/br/pucpr/authserver/users/dto/LoginResponse.java`
- `authserver/src/main/java/br/pucpr/authserver/users/dto/UserResponse.java`
- `authserver/src/main/java/br/pucpr/authserver/users/validation/PasswordPolicy.java`
- `authserver/src/main/java/br/pucpr/authserver/users/validation/PasswordValidator.java`
- `authserver/src/main/java/br/pucpr/authserver/users/validation/ValidPassword.java`
- `authserver/src/main/resources/application-dev.yaml`
- `authserver/src/main/resources/application-prod.yaml`
- `authserver/src/main/resources/application.yaml`
- `authserver/src/test/java/br/pucpr/authserver/config/AppPropertiesTests.java`
- `authserver/src/test/java/br/pucpr/authserver/users/UserServiceTests.java`
- `authserver/src/test/resources/application-test.yaml`

## Arquivos modificados ou substituídos

- M: `README.md`
- M: `authserver/.gitignore`
- M: `authserver/mvnw.cmd`
- M: `authserver/pom.xml`
- M: `authserver/src/main/java/br/pucpr/authserver/AuthserverApplication.java`
- D: `authserver/src/main/resources/application.properties`
- M: `authserver/src/test/java/br/pucpr/authserver/AuthserverApplicationTests.java`
