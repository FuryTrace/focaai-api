# Testes automatizados do FocaAi (API)

## Escopo escolhido

Para a atividade de Testes, Métricas e Qualidade de Software, o módulo candidato é **Autenticação e Gerenciamento de Conta**, com foco nas regras de cadastro, login, alteração de senha, exclusão de conta e recuperação de senha.

Os testes automatizados desta entrega são testes unitários JUnit 5 para as classes `UsuarioService` e `RecuperacaoSenhaService`. Os repositórios e o serviço de e-mail são simulados com Mockito. Portanto, os testes unitários não enviam e-mails reais nem dependem de um banco H2 ou de uma chave real do Resend.

## Como executar no IntelliJ IDEA

1. Extraia o ZIP da API em uma pasta.
2. Abra no IntelliJ IDEA a pasta que contém `build.gradle` e `settings.gradle`.
3. Aguarde o Gradle carregar o projeto. Se solicitado, permita a sincronização.
4. Confira se o projeto está usando JDK 21 (o `build.gradle` declara Java 21).
5. No painel Gradle, abra `Tasks > verification`.
6. Execute a tarefa `test`.
7. Aguarde o resultado. O Gradle deve indicar `BUILD SUCCESSFUL` se todos os testes passarem.

Também é possível executar tudo pelo terminal, na pasta raiz da API:

### Windows (PowerShell)

```powershell
.\gradlew.bat test
```

### Windows (Prompt de Comando)

```bat
gradlew.bat test
```

### Linux/macOS

```bash
./gradlew test
```

## Como ver os resultados

- No IntelliJ, abra a janela **Run** ou **Build** e confira se aparece `BUILD SUCCESSFUL`.
- Os relatórios JUnit XML ficam em `build/test-results/test/`.
- O relatório visual JaCoCo fica em `build/reports/jacoco/test/html/index.html`.
- Abra `index.html` no navegador para visualizar cobertura de linhas e de ramificações (branches).

A tarefa `test` foi configurada para gerar o relatório JaCoCo automaticamente ao terminar.

## O que os testes verificam

### `UsuarioServiceTest`

- Cadastro com e-mail novo e armazenamento da senha em BCrypt.
- Bloqueio de cadastro com e-mail já existente.
- Login com credenciais corretas.
- Rejeição de senha incorreta.
- Rejeição de usuário inexistente.
- Rejeição de senha atual incorreta na alteração.
- Rejeição de nova senha igual à atual.
- Alteração bem-sucedida da senha.
- Exclusão de conta com senha correta.
- Bloqueio da exclusão com senha incorreta.

### `RecuperacaoSenhaServiceTest`

- Solicitação de validação sem registro de recuperação.
- Código já utilizado.
- Código já validado.
- Código expirado.
- Limite de cinco tentativas atingido.
- Código incorreto, com incremento do contador de tentativas.
- Código correto, com geração e armazenamento de token de recuperação.

## Complexidade ciclomática do método selecionado

Método candidato: `RecuperacaoSenhaService.validar(ValidarCodigoRequest)`.

No método há:
- Uma decisão `if` com quatro condições ligadas por `||`.
- Uma segunda decisão `if` que verifica se o código informado corresponde ao hash salvo.

Pela contagem de predicados (dois `if` + três operadores `||`), a complexidade ciclomática é:

**V(G) = 1 + 2 + 3 = 6**

Os seis caminhos independentes considerados são:
1. Código já utilizado.
2. Código ainda não utilizado, mas já validado.
3. Código não utilizado/não validado, porém expirado.
4. Código dentro da validade, mas com cinco tentativas atingidas.
5. Registro válido, mas código informado incorreto.
6. Registro válido e código informado correto.

Os testes `validar_codigoJaUtilizado`, `validar_codigoJaValidado`, `validar_codigoExpirado`, `validar_limiteDeTentativasAtingido`, `validar_codigoIncorreto` e `validar_codigoCorreto` cobrem esses seis caminhos independentes. O teste de recuperação inexistente cobre também o caso de ausência do registro.

**Observação técnica:** JaCoCo apresenta cobertura de linhas e ramificações; não apresenta uma métrica geral de todos os caminhos possíveis. A justificativa dos seis caminhos independentes deve ser apresentada junto do cálculo da complexidade ciclomática no documento da disciplina.

## Importante

- A chave real `RESEND_API_KEY` não é necessária para executar esses testes unitários.
- O teste de inicialização da aplicação usa uma chave fictícia apenas no contexto de teste; não envia e-mail.
- Estes testes não substituem os testes funcionais no Android nem os testes de integração/aceitação da API.
- Não envie chaves reais, senhas ou outros segredos para o GitHub.
