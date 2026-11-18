# AuthServer — autenticação e catálogo de futebol

## Integrantes

- João Vitor Simão Ribas
- Arthur Krauze
- Pedro Henrique Junqueira

## Vídeo de apresentação

Link do YouTube: **[INSERIR LINK APÓS PUBLICAR O VÍDEO]**

## Escopo da entrega

A extensão está na branch joao-ribas/authserver-extensao-futebol. Há quatro entidades novas: League, Team, Player e Championship. Liga–times e time–jogadores representam relações 1:N, mapeadas pelo lado ManyToOne; jogadores–títulos usam ManyToMany com a tabela FootballPlayerTitle. Times e jogadores possuem CRUD completo. Escritas de futebol exigem JWT com papel ADMIN; leituras são públicas. Os serviços registram logs e tratam exceções.

API REST acadêmica em Java 25 e Spring Boot 4.0.9, com autenticação JWT e catálogo de futebol. O catálogo acrescenta quatro entidades, CRUD de times e jogadores, associações, filtros e ordenação.
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

Os recursos do Swagger são infraestrutura de documentação, não endpoints de negócio adicionais. A API inclui as operações de autenticação e as operações de futebol descritas adiante.

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

Validação em 09/11/2026: uma execução de verify concluiu com 50 testes passando. Uma repetição posterior foi interrompida por falta de memória nativa da JVM; não representa uma nova aprovação da suíte.
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

O CRUD de times e jogadores está implementado. Atualização/exclusão de usuários, gerenciamento de papéis, refresh token e revogação não fazem parte do escopo atual.
## Apresentação

Siga o [roteiro de demonstração](#escopo-da-entrega): health → cadastro → validação inválida → login → 401 sem token → listagem autenticada → consulta por ID.

---

# Extensão de futebol

O módulo aproveita o login JWT e o H2 em memória do AuthServer. Para a apresentação, a carga inclui **LaLiga, Premier League, Bundesliga e Brasileirão Série A**, com dois clubes por liga, dois jogadores por clube e um título de demonstração por liga.

O recorte usa nomes reais de clubes. Jogadores, estatísticas, torcidas, valores de mercado e títulos `DEMO-01` são ilustrativos. As datas das ligas europeias representam a abertura da primeira edição. Para o Brasileirão, o exemplo usa o início do Campeonato Nacional de Clubes em 1971. [LaLiga](https://assets.laliga.com/assets/2019/12/17/originals/446f9acd8049c238c01312f5d91a14d3.pdf) · [Premier League](https://www.premierleague.com/en/history) · [Bundesliga](https://datencenter.dfb.de/en/competitions/bundesliga/seasons/1963-1964/matchday?datacenter_name=data-center) · [Brasileirão de 1971](https://www1.folha.uol.com.br/banco-de-dados/2021/08/1971-primeiro-campeonato-nacional-de-futebol-comeca-a-ser-disputado.shtml)

## Como os dados entram no H2

1. Configure Java 25 e as três variáveis do AuthServer descritas no [README](#configuração).
2. Na pasta `authserver`, inicie o perfil `dev`.

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
   ```

3. O Hibernate cria as tabelas. Depois, `FootballSeed` lê [football-demo.json](authserver/src/main/resources/football-demo.json) e grava os registros em uma transação, na ordem liga → time → título → jogador. Um banco que já contém ligas não recebe a carga novamente.
4. Abra [Swagger UI](http://localhost:8080/api/swagger-ui/index.html) ou consulte `GET /api/football/leagues`. Os dados são recriados quando a aplicação reinicia. O perfil `prod` inicia sem a carga de futebol.

Para trocar a amostra, edite o JSON e reinicie no perfil `dev`. Coloque cada clube em `teams` da liga e cada jogador em `players` do clube. Use datas ISO (`AAAA-MM-DD`) e números JSON para as estatísticas e o valor de mercado em euros. O campo opcional `titleSeason` cria um título da liga para aquele clube. `champion: true` vincula o jogador a esse título. Para inserir registros durante a execução, use os endpoints de escrita com JWT de administrador.

## Modelo

| Entidade | Relação |
| --- | --- |
| Liga (`League`) | Uma liga tem muitos times. Cada time pertence a uma liga. |
| Time (`Team`) | Um time tem muitos jogadores. Cada jogador tem um time atual. |
| Jogador (`Player`) | Um jogador pode ter muitos títulos e um título pode ser associado a vários jogadores. A tabela `FootballPlayerTitle` guarda os vínculos. |
| Título (`Championship`) | Identifica o time campeão de uma liga em uma temporada. Cada par liga/temporada tem um campeão. |

O elenco atual é consultado pelo time do jogador. Os títulos conquistados continuam com o jogador após uma transferência. A lista de campeões da liga vem dos títulos registrados e permanece vinculada à liga histórica se o time trocar de competição.

## Endpoints

Todos usam o prefixo `/api/football`. As leituras são públicas. Faça login em `POST /api/users/login` com as credenciais do administrador inicial para alterar dados. No Swagger, use **Authorize** com o token retornado. Por HTTP, envie `Authorization: Bearer <token>`.

| Método | Caminho após `/api/football` | Acesso | Sucesso | Operação |
| --- | --- | --- | --- | --- |
| GET | `/leagues` | Público | 200 | Listar ligas |
| GET | `/leagues/{id}` | Público | 200 | Consultar liga |
| POST | `/leagues` | ADMIN | 201 | Cadastrar liga |
| GET | `/leagues/{id}/teams` | Público | 200 | Listar os times da liga |
| GET | `/leagues/{id}/titles` | Público | 200 | Listar campeões da liga |
| POST | `/leagues/{id}/titles` | ADMIN | 201 | Registrar título da liga |
| GET | `/teams` | Público | 200 | Listar times |
| GET | `/teams/{id}` | Público | 200 | Consultar time |
| POST | `/teams` | ADMIN | 201 | Cadastrar time |
| PUT | `/teams/{id}` | ADMIN | 200 | Atualizar time |
| DELETE | `/teams/{id}` | ADMIN | 204 | Excluir time |
| GET | `/teams/{id}/players` | Público | 200 | Listar o elenco do time |
| GET | `/teams/{id}/titles` | Público | 200 | Listar os títulos do time |
| PUT | `/teams/{id}/league/{leagueId}` | ADMIN | 200 | Associar o time a uma liga |
| GET | `/players` | Público | 200 | Filtrar e ordenar jogadores |
| GET | `/players/{id}` | Público | 200 | Consultar jogador |
| POST | `/players` | ADMIN | 201 | Cadastrar jogador |
| PUT | `/players/{id}` | ADMIN | 200 | Atualizar jogador |
| DELETE | `/players/{id}` | ADMIN | 204 | Excluir jogador |
| PUT | `/players/{id}/team/{teamId}` | ADMIN | 200 | Transferir jogador |
| PUT | `/players/{id}/titles/{titleId}` | ADMIN | 200 | Vincular título ao jogador |
| DELETE | `/players/{id}/titles/{titleId}` | ADMIN | 204 | Remover vínculo com o título |

Exemplo de corpo para cadastrar ou atualizar um jogador. Substitua `teamId` por um ID retornado em `GET /teams`.

```json
{
  "name": "Jogador de exemplo",
  "age": 24,
  "appearances": 32,
  "goals": 14,
  "assists": 8,
  "marketValue": 3500000.00,
  "nationality": "Brasil",
  "teamId": 1
}
```

Para cadastrar ou atualizar um time, use `name`, `foundedOn`, `supporters` e `leagueId`. Para cadastrar uma liga, use `name`, `country` e `foundedOn`. Para registrar um título, envie `season` e `championId`. O campeão precisa pertencer à liga no momento do registro.

## Consultas e rankings

```http
GET /api/football/players?sortBy=goals&sortDir=DESC&limit=5
GET /api/football/players?sortBy=appearances&sortDir=DESC&limit=5
GET /api/football/players?sortBy=assists&sortDir=DESC&limit=5
GET /api/football/players?leagueId=1&nationality=Brasil&sortBy=goals&sortDir=DESC
```

Combine `teamId`, `leagueId` e `nationality`. O filtro de nacionalidade compara o texto sem distinguir maiúsculas de minúsculas. A ordenação aceita `name`, `goals`, `appearances` e `assists`, com direção `ASC` ou `DESC`. O padrão é `name ASC`. O ID resolve empates e `limit` aceita de 1 a 100, com padrão 25. Os rankings usam os totais registrados em cada jogador.

## Segurança e erros

Os serviços de times, jogadores e ligas registram alterações pelos IDs das entidades. A API retorna `ProblemDetail` para falhas. `401` indica autenticação ausente ou inválida e `403` indica usuário autenticado sem o papel ADMIN. Também são tratados parâmetros de ordenação inválidos (`400`), registros inexistentes (`404`), conflitos (`409`) e campos inválidos (`422`). Excluir um time ainda ligado a jogadores ou títulos retorna `409` para preservar os vínculos.

Na apresentação, liste as ligas e um elenco, compare os rankings por gols e assistências, tente excluir sem JWT e depois use o token do administrador para cadastrar, transferir e excluir um jogador. Execute `.\mvnw.cmd clean verify` para rodar os testes de integração com H2.

---

# Arquitetura do AuthServer

O projeto é um monólito modular com um único `pom.xml`. Os módulos de aplicação são `users`, `roles` e `football`. Segurança, tratamento de erros e configuração geral são compartilhados.

| Módulo | Configuração Spring | Casos de uso |
| --- | --- | --- |
| Usuários | `UserModuleConfig` | Cadastro, login, consultas e inicialização do administrador |
| Papéis | `RoleModuleConfig` | Inicialização dos papéis ADMIN e PREMIUM |
| Futebol | `FootballModuleConfig` | Operações de ligas, times, jogadores e títulos. Também configura a carga inicial no perfil de demonstração. |

Os controllers chamam `usecase`. Os casos de uso dependem dos contratos em `gateway`. Cada `ModuleConfig` instancia os casos de uso e liga os contratos aos adaptadores `H2*Gateway`, que vivem em `repository` junto às interfaces Spring Data JPA. Os adaptadores concentram a paginação, as consultas e a tradução de conflitos do H2. Os casos de uso delimitam as transações e atribuem o significado de negócio aos conflitos.

As entidades JPA são compartilhadas entre os casos de uso e os adaptadores. A inicialização do administrador chama o caso de uso de papéis na mesma transação. A carga de futebol acessa os adaptadores pelos gateways.

O [guia de futebol](#extensão-de-futebol) lista os endpoints, permissões e exemplos.

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

---

# Roteiro da apresentação parcial — 5 a 8 minutos

## Preparação

1. Selecione Java 25, configure as três variáveis de ambiente e inicie com Maven conforme o [README](#configuração).
2. Abra [Swagger UI](http://localhost:8080/api/swagger-ui/index.html).
3. Explique que são **cinco dos dez endpoints de negócio**. O administrador inicial já existe, e o H2 mantém os dados somente durante a execução.

Os comandos abaixo usam um segundo terminal com **PowerShell 7**. A senha de demonstração é digitada sem eco e não fica fixa em arquivo:

```powershell
$base = 'http://localhost:8080/api'
$email = "demo.$([Guid]::NewGuid().ToString('N').Substring(0,8))@example.com"
$senha = [Net.NetworkCredential]::new('', (Read-Host 'Senha para o usuário de demonstração' -AsSecureString)).Password
```

Use ao menos 8 caracteres, letra, número e símbolo entre `@$!%*#?&`.

## 1. Health sem autenticação

```powershell
Invoke-RestMethod "$base/health"
```

Esperado: HTTP **200**, `{"status":"OK"}`.

## 2. Cadastro

```powershell
$cadastro = @{email=$email; password=$senha; name='Ana'} | ConvertTo-Json
$resposta = Invoke-WebRequest "$base/users" -Method Post -ContentType 'application/json' -Body $cadastro
$resposta.StatusCode
$usuario = $resposta.Content | ConvertFrom-Json
$usuario
```

Esperado: **201**, com id, email e name. Mostre o header Location e observe que senha e hash não aparecem. O ID é gerado pelo banco; use o retornado.

## 3. Tentativa de cadastro inválido

```powershell
$invalido = @{email='email-invalido'; password='123'; name=''} | ConvertTo-Json
$erro = Invoke-WebRequest "$base/users" -Method Post -ContentType 'application/json' -Body $invalido -SkipHttpErrorCheck
$erro.StatusCode
$erro.Content
```

Esperado: **422**, `application/problem+json` e `errors` identificando email, password e name. Repetir o cadastro válido com o mesmo e-mail também demonstra **409**.

## 4. Login e geração de JWT

```powershell
$credenciais = @{email=$email; password=$senha} | ConvertTo-Json
$login = Invoke-RestMethod "$base/users/login" -Method Post -ContentType 'application/json' -Body $credenciais
$login.user
"JWT recebido: $(-not [string]::IsNullOrWhiteSpace($login.token))"
$headers = @{Authorization="Bearer $($login.token)"}
```

Esperado: **200**, token e usuário. Não imprima nem registre o token completo. Explique que o servidor verifica HS256, emissor e expiração; usuários comuns têm 48 horas, administradores 1 hora.

## 5. Consulta sem token

```powershell
$erro = Invoke-WebRequest "$base/users" -SkipHttpErrorCheck
$erro.StatusCode
$erro.Content
```

Esperado: **401**, com ProblemDetail. A API não cria sessão após o login.

## 6. Listagem com JWT

```powershell
Invoke-RestMethod "$base/users?sortDir=ASC" -Headers $headers
Invoke-RestMethod "$base/users?sortDir=DESC" -Headers $headers
Invoke-RestMethod "$base/users?role=ADMIN" -Headers $headers
```

Esperado: **200**, ordenação por nome sem distinguir caixa e filtro de papel. Um usuário comum autenticado pode consultar a lista. Não há endpoint público para conceder ADMIN.

## 7. Consulta por ID

```powershell
Invoke-RestMethod "$base/users/$($usuario.id)" -Headers $headers
```

Esperado: **200** com os mesmos dados públicos do cadastro. Opcionalmente mostre `/users/999999` → **404**, `/users/abc` → **400** e `/users/0` → **422**.

## Encerramento

Mostre o resultado de `.\mvnw.cmd verify` (ou `./mvnw verify`) e os testes em `src/test/java`. Apresente os pacotes config, security, exceptions, users e roles. Explique o limite da entrega: gerenciamento de papéis e as cinco rotas restantes ficam para a segunda etapa.

As senhas são persistidas apenas como BCrypt, as respostas usam records e as relações LAZY são acessadas dentro das transações. Não há credenciais reais no repositório.

Ao terminar, encerre o servidor com Ctrl+C e feche os terminais de demonstração para descartar os valores mantidos em memória.

---

# Maven e execução no IntelliJ IDEA (Windows)

## Diagnóstico verificado em 09/11/2026

O repositório contém um único projeto Maven, em `authserver/pom.xml`. A pasta
superior não tem POM agregador. Os pacotes internos de usuários, papéis e futebol
pertencem ao mesmo módulo Maven; não exigem reestruturação.

O log local do IntelliJ IDEA 2026.1.4 continha a causa completa:

```text
[Guice/MissingImplementation]: No implementation for ArtifactHandlerManager was bound.
Requested by: DefaultArtifactFactory.artifactHandlerManager
at DefaultPlexusContainer$1.configure
```

A falha ocorreu no servidor Maven da IDE com o wrapper Maven 3.10.0, antes da
compilação. O Maven 3.10.0 funcionou pelo terminal com JDK 25. A importação na IDE
terminou com sucesso após selecionar **Bundled (Maven 3)**, versão local 3.9.11.
Isso localiza o problema na integração dessa IDE com Maven 3.10.0, sem indicar
conflito entre as dependências da aplicação. O wrapper foi preservado.

Havia também configurações Java divergentes: `JAVA_HOME` do terminal apontava
para Zulu 8; o SDK do projeto e o Maven Runner usavam Java 22; o nível de linguagem
do projeto era 26 e o módulo exibia 5 durante a importação incompleta. O POM exige
Java 25. A classe `br.pucpr.authserver.AuthserverApplication` tem o método
`public static void main(String[] args)` correto e não precisou de alteração.

## Abrir e importar

1. Use **File > Open**, selecione `authserver/pom.xml` e abra como projeto Maven.
   Se já abriu a pasta superior, clique com o botão direito nesse POM e escolha
   **Add as Maven Project**; mantenha apenas esse POM vinculado.
2. Em **File > Project Structure > Project**, configure **SDK: JDK 25** e
   **Language level: 25**, sem preview. Há instalações locais em
   `E:\JDK\jdk-25\jdk-25.0.4.1` e
   `C:\Users\joao.ribas\.jdks\ms-25.0.3`; em outra máquina, selecione seu JDK 25.
3. Em **Settings > Build, Execution, Deployment > Build Tools > Maven**, selecione
   **Maven home path: Bundled (Maven 3)** nesta versão da IDE. Isso mantém o
   wrapper Maven 3.10.0 para o terminal. Não é necessário alterar o POM.
4. Em **Maven > Importing**, selecione **JDK for importer: 25**.
5. Em **Maven > Runner**, selecione **JRE: Project JDK** (agora 25) ou JDK 25
   explicitamente. Não use `JAVA_HOME` enquanto ele apontar para Java 8.
6. Na janela Maven, execute **Reload All Maven Projects** e aguarde também a
   indexação. Na validação local a sincronização levou cerca de cinco minutos;
   houve avisos de download de BOM nos repositórios Spring snapshot/milestone,
   mas a sincronização terminou com sucesso.
7. Em **Project Structure > Modules > authserver**, confira o SDK herdado do
   projeto e linguagem 25. A importação deve reconhecer `src/main/java` como
   **Sources**, `src/main/resources` como **Resources**, `src/test/java` como
   **Tests** e `src/test/resources` como **Test Resources**. `target` é excluído.
   A pasta `src/main/java` foi observada como Source Folder na IDE local.

Se a sincronização falhar, consulte **Help > Show Log in Explorer**, arquivo
`idea.log`, e copie a primeira causa e o stacktrace completo. A legenda de nomes
de classes, sozinha, não diagnostica a falha. Não apague todo o cache `.m2` nem
adicione Plexus/Guice como dependências da aplicação para corrigir o importador.

## Executar pela IDE

1. Abra `AuthserverApplication.java` e use o ícone verde ao lado do `main`.
   Com suporte Spring instalado, uma configuração **Spring Boot** também serve.
2. Para configurar manualmente, use **Run > Edit Configurations > + > Application**:

   | Campo | Valor |
   | --- | --- |
   | Main class | `br.pucpr.authserver.AuthserverApplication` |
   | Use classpath of module | `authserver` |
   | JRE | JDK 25 / SDK do módulo |
   | Working directory | caminho absoluto da pasta `authserver` |
   | Program arguments | `--spring.profiles.active=dev` |

   Informe o nome qualificado da classe, sem `.java`. Na configuração **Spring
   Boot**, pode preencher **Active profiles: dev** no lugar do argumento.
3. Em **Modify options > Environment variables**, defina localmente:
   - `APP_SECURITY_SECRET`: segredo aleatório com pelo menos 32 bytes UTF-8;
   - `APP_ADMIN_EMAIL`: e-mail válido do administrador local;
   - `APP_ADMIN_PASSWORD`: senha com 8 caracteres ou mais, letra, número e um dos
     símbolos `@$!%*#?&`, limitada a 72 bytes UTF-8 e à política do projeto.

   Use o [README](#configuração) para os detalhes. O perfil `dev`
   também carrega os dados de demonstração de futebol. O perfil `test` e suas
   credenciais fictícias são exclusivos dos testes. Spring não lê `.env`
   automaticamente. As variáveis definidas em um terminal não são herdadas por
   uma IDE que já estava aberta; preencha a configuração da IDE diretamente.
4. Mantenha **Store as project file** desmarcado e confirme com **OK**. Não
   versione credenciais ou arquivos `.idea`.
5. Execute e aguarde `Started AuthserverApplication`. Confira
   `http://localhost:8080/api/health`, que deve responder `{"status":"OK"}`.
   Swagger: `http://localhost:8080/api/swagger-ui/index.html`.

Não são necessários banco externo, Docker ou outros microsserviços para essa
configuração: ela usa H2 em memória, com criação/remoção do esquema. Os dados
são descartados ao encerrar. A porta padrão é 8080; se ocupada, configure
`--server.port=18080` e ajuste a URL de teste.

Se ocorrer especificamente `Unable to establish loopback connection` no Windows,
crie a pasta `target` e adicione às **VM options**:
`-Djdk.net.unixdomain.tmpdir=CAMINHO_ABSOLUTO_PARA_AUTHServer\target`.
O POM já aplica esse ajuste a `spring-boot:run` e aos testes Maven; a configuração
Application da IDE não herda os argumentos JVM do plugin Maven.

## Validação pelo terminal

No PowerShell, dentro da pasta `authserver`:

```powershell
$env:JAVA_HOME = 'C:\Users\joao.ribas\.jdks\ms-25.0.3'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
.\mvnw.cmd clean compile
.\mvnw.cmd verify
```

`JAVA_HOME` acima vale apenas para a sessão. Substitua pelo seu JDK 25 quando
necessário; não basta mudar apenas o `Path`, pois o Maven consulta `JAVA_HOME`.

Resultados desta investigação:

| Verificação | Resultado |
| --- | --- |
| Wrapper `-version`, ambiente original | Maven 3.10.0 / Java 8: JDK incorreto |
| Wrapper `clean compile`, ambiente original | Falhou em sintaxe moderna (records, text blocks etc.) com Java 8 |
| Wrapper `-version`, JDK 25 | Maven 3.10.0 / Microsoft Java 25.0.3 |
| Wrapper `clean compile`, JDK 25 | BUILD SUCCESS; 64 fontes Java |
| Wrapper `verify`, JDK 25 | BUILD SUCCESS; 50 testes, zero falhas/erros/ignorados; JAR reempacotado |
| Maven integrado 3.9.11, `-o validate`, Oracle JDK 25 | BUILD SUCCESS |
| JAR executável, perfil dev | Iniciou com Java 25.0.3, H2 e Tomcat; HTTP 200 no health check |
| Importação no IntelliJ | Sync concluído com sucesso usando Maven integrado |
| Run/Debug Configurations | Classe, módulo e Java 25 reconhecidos; OK habilitado; perfil dev salvo |

O JAR foi iniciado com credenciais fictícias geradas apenas no processo, endereço
`127.0.0.1` e porta `18080`, usando:

```powershell
java '-Djdk.net.unixdomain.tmpdir=E:\pucpr\authserver\backend-service-pucpr\authserver\target' -jar target/authserver-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.address=127.0.0.1 --server.port=18080
Invoke-WebRequest http://127.0.0.1:18080/api/health
```

As três variáveis obrigatórias foram definidas antes desse comando, sem gravar
seus valores em arquivos. O processo foi encerrado após o teste. A execução pelo
botão Run da IDE com credenciais do usuário não foi realizada; somente a
importação e a configuração de execução foram validadas na interface local.

Uma execução inicial no sandbox com JDK 25 apresentou erro ao ler `java.security`.
O arquivo existe; a compilação e os testes passaram fora do sandbox. Esse erro
foi uma restrição do ambiente de diagnóstico e é diferente da falha
`ArtifactHandlerManager` registrada pelo IntelliJ.

Nenhuma versão, regra de negócio, credencial existente ou configuração de
produção foi alterada. As configurações locais da IDE ficam em `.idea`, ignorado
na raiz do repositório. Não foi feito commit ou push.

Referências: [Maven no IntelliJ](https://www.jetbrains.com/help/idea/maven-support.html),
[JDK do importador](https://www.jetbrains.com/help/idea/maven-importing.html),
[Maven Runner](https://www.jetbrains.com/help/idea/maven-runner.html),
[suporte Java 25 no Spring Boot 4](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/).

## Histórico da primeira entrega

O relatório abaixo documenta exclusivamente a entrega inicial de autenticação de 29/10/2026. As contagens de 48 testes e cinco operações são históricas; o catálogo de futebol é posterior.

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

As soluções para SpringExtension/JUnit 5, coexistência de Jackson, Maven Wrapper Windows e sockets do JDK estão detalhadas no [README](#configuração). O Mockito é carregado como agente explícito apenas nos testes.

## Limites e execução

H2 é em memória e perde os dados ao encerrar. Não há endpoints da segunda etapa, gerenciamento de papéis, atualização/exclusão, paginação, refresh ou revogação. Não restaram bloqueios de compilação ou testes.

Para executar, selecione JDK 25, configure `APP_SECURITY_SECRET`, `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`, e use `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'`. Consulte os comandos completos no [README](#configuração).

O [roteiro da apresentação](#histórico-da-primeira-entrega) demonstra os sete passos: health, cadastro, erro 422, login, erro 401, listagem com JWT e consulta por ID.

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
