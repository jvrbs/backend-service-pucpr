# Extensão de futebol

O módulo aproveita o login JWT e o H2 em memória do AuthServer. Para a apresentação, a carga inclui **LaLiga, Premier League, Bundesliga e Brasileirão Série A**, com dois clubes por liga, dois jogadores por clube e um título de demonstração por liga.

O recorte usa nomes reais de clubes. Jogadores, estatísticas, torcidas, valores de mercado e títulos `DEMO-01` são ilustrativos. As datas das ligas europeias representam a abertura da primeira edição. Para o Brasileirão, o exemplo usa o início do Campeonato Nacional de Clubes em 1971. [LaLiga](https://assets.laliga.com/assets/2019/12/17/originals/446f9acd8049c238c01312f5d91a14d3.pdf) · [Premier League](https://www.premierleague.com/en/history) · [Bundesliga](https://datencenter.dfb.de/en/competitions/bundesliga/seasons/1963-1964/matchday?datacenter_name=data-center) · [Brasileirão de 1971](https://www1.folha.uol.com.br/banco-de-dados/2021/08/1971-primeiro-campeonato-nacional-de-futebol-comeca-a-ser-disputado.shtml)

## Como os dados entram no H2

1. Configure Java 25 e as três variáveis do AuthServer descritas no [README](../README.md).
2. Na pasta `authserver`, inicie o perfil `dev`.

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
   ```

3. O Hibernate cria as tabelas. Depois, `FootballSeed` lê [football-demo.json](../src/main/resources/football-demo.json) e grava os registros em uma transação, na ordem liga → time → título → jogador. Um banco que já contém ligas não recebe a carga novamente.
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
