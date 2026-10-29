# AuthServer — primeira entrega parcial

API REST acadêmica em Java. Esta entrega implementa **5 dos 10 endpoints de negócio** previstos: cadastro, login, listagem, consulta por ID e health check. O projeto evolui a estrutura original do Spring Initializr e mantém Maven, Java 25 e Spring Boot 4.0.9.

## Tecnologias e requisitos

- JDK **25** (configure `JAVA_HOME`; `java -version` deve indicar 25).
- Spring Boot **4.0.9**, Spring MVC, Spring Security, Spring Data JPA e Jakarta Bean Validation.
- H2 em memória e Hibernate; Maven **3.10.0** pelo wrapper incluído.
- JJWT **0.13.0**, assinatura HS256 e BCrypt com custo 12.
- Springdoc OpenAPI **3.0.3**, Swagger UI.
- JUnit Jupiter **5.14.4**, Mockito e MockMvc.
- Acesso ao Maven Central na primeira execução para baixar dependências.

Não é necessário instalar Maven globalmente. Não há Kotlin, Lombok, Gradle, Docker, AWS, OAuth2 ou refresh token.

## Configuração

As três variáveis abaixo são obrigatórias fora dos testes. A aplicação falha ao iniciar se estiverem ausentes ou inválidas.

| Variável | Conteúdo |
| --- | --- |
| `APP_SECURITY_SECRET` | Segredo aleatório de pelo menos **32 bytes UTF-8**; usado como bytes textuais da chave HS256, sem decodificação Base64 |
| `APP_ADMIN_EMAIL` | E-mail válido do administrador inicial; normalizado com trim e lowercase |
| `APP_ADMIN_PASSWORD` | Senha do administrador conforme a mesma política do cadastro |

A política de senha exige pelo menos 8 caracteres, letra, número e um dos símbolos `@$!%*#?&`. Só são aceitos letras, números e esses símbolos; letras Unicode são permitidas. O limite é **72 bytes UTF-8**, não 72 caracteres. E-mails têm limite de 254 caracteres e nomes de 255.

As propriedades próprias são agrupadas em `AppProperties`, com validação na inicialização:

- `app.security.issuer=authserver`;
- `app.security.token-ttl=48h`;
- `app.security.admin-token-ttl=1h`;
- `app.admin.email` e `app.admin.password`.

Arquivos de configuração:

| Arquivo | Finalidade |
| --- | --- |
| `src/main/resources/application.yaml` | Configuração comum, prefixo `/api`, H2, JPA e variáveis obrigatórias |
| `src/main/resources/application-dev.yaml` | Desenvolvimento, sem log SQL |
| `src/main/resources/application-prod.yaml` | Perfil de execução prod, encerramento gracioso e logs reduzidos |
| `src/test/resources/application-test.yaml` | Banco separado e valores **fictícios exclusivos dos testes** |

O perfil `test` fica nos recursos de teste e **não é empacotado no JAR**. Arquivos `.env` são ignorados pelo Git; Spring Boot não os carrega automaticamente.

O Bootstrapper cria ADMIN e PREMIUM e cadastra o administrador configurado. Reexecutá-lo não duplica usuários, papéis ou vínculos e não altera a senha existente. O cadastro público cria usuários sem papéis; não há operação pública para atribuir ADMIN.

## Executar localmente

No diretório `authserver/`, use PowerShell 7:

```powershell
# Se necessário, selecione a instalação local do JDK 25:
$env:JAVA_HOME = 'C:\caminho\para\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Gera um segredo novo apenas para esta sessão.
$env:APP_SECURITY_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:APP_ADMIN_EMAIL = Read-Host 'E-mail do administrador'
$env:APP_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Senha do administrador' -AsSecureString)).Password

.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Linux/macOS/Git Bash, com JDK 25 selecionado:

```bash
export APP_SECURITY_SECRET="$(openssl rand -base64 48)"
read -r -p "E-mail do administrador: " APP_ADMIN_EMAIL
read -r -s -p "Senha do administrador: " APP_ADMIN_PASSWORD; echo
export APP_ADMIN_EMAIL APP_ADMIN_PASSWORD
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API fica em [http://localhost:8080/api](http://localhost:8080/api). O segredo deve permanecer estável enquanto os tokens emitidos precisarem continuar válidos. Nenhuma senha ou chave real deve ser adicionada aos arquivos do projeto.

Também é possível executar o pacote gerado:

```bash
java -jar target/authserver-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Se o Windows apresentar `Unable to establish loopback connection`, execute o JAR com `java "-Djdk.net.unixdomain.tmpdir=CAMINHO_ABSOLUTO_PARA_TARGET" -jar target/authserver-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`. O diretório deve existir. As execuções pelo Maven já recebem esse ajuste automaticamente.

O perfil prod continua usando H2 em memória, conforme o escopo acadêmico: **os dados são perdidos ao encerrar o processo**. Ele não representa uma configuração de persistência durável para produção.

## Endpoints

O prefixo é configurado por `server.servlet.context-path=/api`; os controllers não repetem `/api`.

| Método | Endpoint | Acesso | Sucesso |
| --- | --- | --- | --- |
| POST | `/api/users` | Público | 201 |
| POST | `/api/users/login` | Público | 200 |
| GET | `/api/users` | JWT válido | 200 |
| GET | `/api/users/{id}` | JWT válido | 200 |
| GET | `/api/health` | Público | 200 |

### Cadastro

```http
POST /api/users
Content-Type: application/json

{"email":"ana@email.com","password":"Senha@123","name":"Ana"}
```

Os valores acima são exemplos fictícios, sem uso na configuração da aplicação.

Resposta `201 Created`, com `Location: http://localhost:8080/api/users/2`:

```json
{"id":2,"email":"ana@email.com","name":"Ana"}
```

O identificador depende dos dados existentes; normalmente o administrador é criado primeiro. E-mail duplicado, inclusive após normalização, retorna 409. Campos ausentes ou inválidos retornam 422. Uma restrição única no banco também impede duplicidade em cadastros concorrentes.

### Login

```http
POST /api/users/login
Content-Type: application/json

{"email":"ana@email.com","password":"Senha@123"}
```

Resposta `200 OK`:

```json
{
  "token": "<JWT_GERADO>",
  "user": {"id":2,"email":"ana@email.com","name":"Ana"}
}
```

Senha incorreta e e-mail inexistente retornam a mesma resposta 401. A comparação BCrypt também é executada para usuários inexistentes, usando um hash de referência gerado em memória.

O JWT contém `iss`, `sub` (ID textual do usuário), `iat`, `exp` e `user: {id, name, roles}`. A validade é 48 horas para usuários comuns e 1 hora para ADMIN. A verificação exige HS256, assinatura válida, emissor correto e claims obrigatórias; tokens expirados são rejeitados.

### Listagem

```http
GET /api/users?sortDir=ASC&role=ADMIN
Authorization: Bearer <JWT_GERADO>
```

Resposta `200 OK` (e-mail ilustrativo):

```json
[{"id":1,"email":"admin@example.com","name":"Administrador"}]
```

- Ordenação padrão ASC pelo nome, sem diferenciar maiúsculas e minúsculas; DESC inverte a direção. ID desempata nomes iguais.
- `sortDir` aceita ASC/DESC em qualquer caixa; outros valores retornam 400.
- `role` é opcional, normalizado para maiúsculas. Um papel inexistente retorna lista vazia.
- A consulta é permitida a qualquer usuário autenticado; o filtro não concede papéis.

### Consulta por ID

```http
GET /api/users/2
Authorization: Bearer <JWT_GERADO>
```

Resposta `200 OK`:

```json
{"id":2,"email":"ana@email.com","name":"Ana"}
```

ID inexistente retorna 404; não numérico ou fora do intervalo de Long retorna 400; ID menor que 1 retorna 422.

### Health

```http
GET /api/health
```

Resposta `200 OK` sem autenticação:

```json
{"status":"OK"}
```

## Erros

Erros seguem `ProblemDetail`/RFC 9457, com `Content-Type: application/problem+json`. Exemplo para cadastro sem campos:

```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/api/users",
  "errors": {
    "email": ["não deve estar em branco"],
    "password": ["não deve estar em branco"],
    "name": ["não deve estar em branco"]
  }
}
```

As mensagens de Bean Validation seguem o locale da execução; os nomes dos campos e os códigos HTTP são estáveis.

Exemplo de consulta sem token:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Autenticação necessária ou token inválido.",
  "instance": "/api/users"
}
```

Exemplo de conflito:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "E-mail já cadastrado.",
  "instance": "/api/users"
}
```

| Código | Situação |
| --- | --- |
| 400 | JSON malformado, parâmetro inválido ou ID não numérico |
| 401 | Credenciais incorretas, JWT ausente ou inválido |
| 403 | Falta de permissão, tratada pelo AccessDeniedHandler; não há rota exclusiva de ADMIN nesta etapa |
| 404 | Usuário ou recurso inexistente |
| 409 | E-mail duplicado |
| 422 | Validação de campos ou ID menor que 1 |
| 500 | Falha inesperada, com mensagem genérica |

Métodos não implementados retornam 405 quando o caminho existe; conteúdo incompatível retorna 415. Esses erros também são padronizados. Stacktraces, valores rejeitados, senhas e tokens não são enviados nos erros.

## Segurança e persistência

- Autenticação stateless, sem sessão, form login, Basic ou endpoint de logout.
- O cliente envia `Authorization: Bearer <token>`. Um header de autenticação inválido é rejeitado mesmo em uma rota pública; basta omiti-lo nessas rotas.
- Somente cadastro, login, health e recursos de documentação são públicos. As demais rotas exigem autenticação por padrão.
- CSRF está desabilitado para a API stateless, que usa o header Bearer em vez de cookies de autenticação.
- Senhas são armazenadas apenas como hash BCrypt. DTOs de resposta nunca expõem a entidade nem seu hash.
- Entidades convencionais `User` e `Role`; DTOs em records; dependências por construtor.
- Tabelas `UserTable`, `Role` e `UserRole` (`idUser`, `idRole`). H2 normaliza identificadores não delimitados para maiúsculas.
- Associação ManyToMany LAZY, sem Open Session in View. Serviços e bootstrap delimitam suas transações; os papéis do JWT são lidos dentro da transação de login.
- O token representa os papéis no momento da emissão. Não há revogação, refresh ou gerenciamento de papéis nesta etapa.

## Swagger UI

- [Swagger UI](http://localhost:8080/api/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:8080/api/v3/api-docs)

Execute o login, copie o campo `token` e use **Authorize** no esquema `bearerAuth`, informando apenas o token (sem repetir `Bearer`). As operações GET de usuários declaram a autenticação; as três operações públicas não a exigem. O Swagger não persiste a autorização entre sessões.

Os recursos do Swagger são infraestrutura de documentação, não endpoints de negócio adicionais. A especificação descreve exatamente cinco operações em quatro caminhos.

## Testes e compilação

Sem definir credenciais externas, com JDK 25 selecionado:

```powershell
.\mvnw.cmd clean verify
```

Ou em Linux/macOS/Git Bash:

```bash
./mvnw clean verify
```

O Maven compila com `--release 25`, executa os testes e empacota o JAR executável em `target/authserver-0.0.1-SNAPSHOT.jar`. Os relatórios ficam em `target/surefire-reports/`.

Resultado da entrega: **48 testes passando**, compilação e empacotamento concluídos e verificação HTTP do JAR aprovada. O [relatório de validação](docs/ENTREGA.md) registra o ambiente, os resultados e o inventário de arquivos criados/modificados.

A suíte usa o contexto real da aplicação, H2, BCrypt e JJWT, com MockMvc conectado ao Spring Security. Os métodos de teste não envolvem as requisições em uma transação externa, para que erros de LAZY e de commit sejam detectados. Inclui:

- cadastro, normalização, duplicidade, campos ausentes, política de senha e limites UTF-8;
- login, claims, assinatura HS256 e validades de 48h/1h;
- token ausente, inválido, expirado, emissor/assinatura/algoritmo incorretos e claims ausentes;
- consultas com usuário comum, ordenação, filtro de papéis e erros de ID;
- health público, documentação e ausência das rotas da segunda etapa;
- idempotência do bootstrap, configuração validada, tratamento 403/500 e conflito concorrente simulado com Mockito.

### Compatibilidade de bibliotecas

1. **JUnit 5 e Spring Boot 4.0.9:** o Boot gerencia JUnit 6, e o `SpringExtension` do Spring Framework 7 exige JUnit 6. Usá-lo com JUnit 5 provoca `NoSuchMethodError` em `ExtensionContext.Store.computeIfAbsent`. Para preservar o requisito JUnit 5, o POM fixa 5.14.4 e os testes abrem/fecham o contexto Boot em `@BeforeAll/@AfterAll`, conectando MockMvc à cadeia real de segurança. Não há downgrade do Spring nem implementação simulada da aplicação. Veja [SpringExtension](https://docs.spring.io/spring-framework/docs/7.0.5/javadoc-api/org/springframework/test/context/junit/jupiter/SpringExtension.html) e [configuração do MockMvc com Security](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/setup.html).
2. **Springdoc:** versão 3.0.3 da linha 3.0.x, correspondente ao Boot 4.0.x na [matriz oficial](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
3. **Jackson:** Boot 4 usa Jackson 3 na API MVC; JJWT 0.13.0 usa seu módulo `jjwt-jackson` com Jackson 2. Os namespaces coexistem; o ObjectMapper do Spring não é passado ao JJWT. As versões transitivas permanecem gerenciadas pelo Boot.
4. **Maven Wrapper Windows:** corrigido o acesso a `Target[0]` quando `.m2` é um diretório sem destino de link. Mantidos o wrapper original e Maven 3.10.0.
5. **Mockito e Java 25:** o Surefire inicia a JVM com o agente Mockito explicitamente, evitando a anexação dinâmica de agentes.
6. **Sockets no Windows:** o JDK local apresentou `Unable to establish loopback connection` ao usar o diretório temporário padrão. Um teste isolado de `Selector.open()` confirmou que um diretório do projeto resolve a falha. Surefire e `spring-boot:run` configuram `jdk.net.unixdomain.tmpdir` para `target`, mantendo o Java 25 e a implementação real do servidor.

## Segunda etapa e limites

Ficam para a segunda entrega as cinco rotas restantes da especificação completa, incluindo o gerenciamento de papéis e as demais operações sobre usuários. Os caminhos e contratos dessas rotas não foram fornecidos nesta tarefa e não foram inventados.

Não foram implementados endpoints adicionais de negócio, atualização/exclusão, atribuição de papéis, refresh token ou revogação. A base H2 é volátil, e a listagem é completa, sem paginação, conforme o contrato desta etapa. O health check informa disponibilidade da aplicação, sem diagnóstico externo.

## Apresentação

Siga o [roteiro de demonstração](docs/DEMO.md): health → cadastro → validação inválida → login → 401 sem token → listagem autenticada → consulta por ID.
