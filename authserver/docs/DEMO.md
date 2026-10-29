# Roteiro da apresentação parcial — 5 a 8 minutos

## Preparação

1. Selecione Java 25, configure as três variáveis de ambiente e inicie com Maven conforme o [README](../README.md).
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
