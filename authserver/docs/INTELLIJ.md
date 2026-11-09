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

   Use o [README](../README.md#configuração) para os detalhes. O perfil `dev`
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
