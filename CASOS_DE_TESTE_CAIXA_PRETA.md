# Projeto FocaAi — Casos de Teste Caixa-Preta

## Módulo e escopo

**Módulo:** Autenticação e Gerenciamento de Conta (API REST).

**Dentro do escopo:** cadastro, login, alteração de senha e solicitação/validação de recuperação de senha.

**Fora do escopo destes casos:** interface visual Android, cronômetro Pomodoro, disciplinas, histórico local e integração real com o provedor de e-mail.

## Matriz de Rastreabilidade de Requisitos (RTM)

| Requisito | User Story / regra de negócio | Casos relacionados |
|---|---|---|
| RF-01 | Como visitante, quero cadastrar uma conta com nome, e-mail válido e senha com pelo menos 8 caracteres, para acessar o FocaAi. | CT-01, CT-02, CT-03 |
| RF-02 | Como usuário cadastrado, quero entrar com meu e-mail e senha corretos, para acessar minha conta. | CT-04, CT-05, CT-06 |
| RF-03 | Como usuário autenticado, quero alterar minha senha informando a senha atual e uma nova senha diferente, para manter minha conta segura. | CT-07, CT-08, CT-09 |
| RF-04 | Como usuário, quero solicitar e validar um código temporário para recuperar minha senha. | CT-10 |

## Especificação dos casos de teste

### CT-01 — Cadastro com dados válidos

- **Requisito:** RF-01
- **Pré-condições:** API em execução; e-mail ainda não cadastrado.
- **Passos:** 1. Enviar `POST /api/usuarios/cadastro`. 2. Informar nome válido, e-mail válido e senha com 8 ou mais caracteres.
- **Dados:** nome `Ana Teste`; e-mail exclusivo; senha `SenhaForte123`.
- **Resultado esperado:** HTTP 201; resposta com identificador, nome e e-mail; senha não deve ser retornada nem armazenada em texto puro.

### CT-02 — Cadastro com e-mail já cadastrado

- **Requisito:** RF-01
- **Pré-condições:** já existe uma conta com o e-mail informado.
- **Passos:** enviar novamente `POST /api/usuarios/cadastro` com o mesmo e-mail.
- **Resultado esperado:** HTTP 409 (Conflict); nenhuma segunda conta deve ser criada.

### CT-03 — Cadastro com senha abaixo do mínimo

- **Requisito:** RF-01
- **Pré-condições:** API em execução.
- **Passos:** enviar `POST /api/usuarios/cadastro` com nome e e-mail válidos, mas senha com 7 caracteres.
- **Resultado esperado:** validação rejeita a requisição; nenhuma conta é criada. A API usa `@Size(min = 8)` no DTO.

### CT-04 — Login com credenciais corretas

- **Requisito:** RF-02
- **Pré-condições:** conta cadastrada e ativa.
- **Passos:** enviar `POST /api/usuarios/login` com e-mail e senha corretos.
- **Resultado esperado:** HTTP 200 e dados públicos do usuário; senha não deve aparecer na resposta.

### CT-05 — Login com senha incorreta

- **Requisito:** RF-02
- **Pré-condições:** conta cadastrada.
- **Passos:** enviar `POST /api/usuarios/login` com e-mail existente e senha incorreta.
- **Resultado esperado:** HTTP 401 (Unauthorized); acesso não concedido.

### CT-06 — Login com e-mail inexistente

- **Requisito:** RF-02
- **Pré-condições:** não existe conta com o e-mail informado.
- **Passos:** enviar `POST /api/usuarios/login` com e-mail não cadastrado.
- **Resultado esperado:** HTTP 401; mensagem genérica de credenciais inválidas.

### CT-07 — Alteração com senha atual incorreta

- **Requisito:** RF-03
- **Pré-condições:** conta cadastrada.
- **Passos:** enviar `PUT /api/usuarios/alterar-senha` com e-mail correto, senha atual incorreta e nova senha válida.
- **Resultado esperado:** HTTP 401; senha armazenada permanece inalterada.

### CT-08 — Tentativa de reutilizar a senha atual

- **Requisito:** RF-03
- **Pré-condições:** conta cadastrada; senha atual conhecida.
- **Passos:** enviar `PUT /api/usuarios/alterar-senha` com senha atual e nova senha iguais.
- **Resultado esperado:** HTTP 400 (Bad Request); alteração não realizada.

### CT-09 — Alteração de senha bem-sucedida

- **Requisito:** RF-03
- **Pré-condições:** conta cadastrada; senha atual conhecida.
- **Passos:** enviar `PUT /api/usuarios/alterar-senha` com senha atual correta e nova senha diferente, com pelo menos 8 caracteres. Em seguida, tentar login com a senha nova.
- **Resultado esperado:** HTTP 204 na alteração; login posterior com a nova senha retorna HTTP 200; senha antiga deixa de autenticar.

### CT-10 — Solicitação de recuperação para e-mail não cadastrado

- **Requisito:** RF-04
- **Pré-condições:** API em execução; e-mail não cadastrado.
- **Passos:** enviar `POST /api/usuarios/recuperacao/solicitar` com o e-mail não cadastrado.
- **Resultado esperado:** HTTP 200 e mensagem genérica que não revela se a conta existe; nenhum código é enviado ou registro de recuperação criado.

## Técnicas de projeto de testes

### Partição de Equivalência

Os dados de entrada foram divididos em classes que devem produzir comportamento equivalente:

| Campo | Classe válida | Classe inválida |
|---|---|---|
| E-mail no cadastro/login | Formato válido e cadastrado (login) ou ainda não cadastrado (cadastro) | Formato inválido, vazio ou e-mail inexistente no login |
| Senha no cadastro | 8 ou mais caracteres | Vazia ou com menos de 8 caracteres |
| Senha atual | Corresponde à senha armazenada | Não corresponde |
| Nova senha | Diferente da atual e com pelo menos 8 caracteres | Igual à atual, vazia ou menor que 8 |
| Código de recuperação | Seis dígitos corretos, dentro da validade e dentro do limite de tentativas | Incorreto, expirado, já utilizado, já validado ou após cinco tentativas |

### Análise de Valor Limite

| Regra | Limite válido | Limite inválido a testar |
|---|---|---|
| Tamanho mínimo da senha | 8 caracteres | 7 caracteres |
| Código de recuperação | Exatamente 6 dígitos | 5 ou 7 dígitos |
| Tentativas de código | Até 4 tentativas anteriores; a quinta ainda é avaliada pelo fluxo de validação | A partir de 5 tentativas, a validação é bloqueada |
| Validade do código | Instante anterior ao horário de expiração | Instante igual ou posterior à expiração |

**Nota:** a implementação verifica `expiraEm.isBefore(Instant.now())`; por isso, um código cujo instante de expiração seja igual ao instante atual não é tratado como expirado por essa condição. O teste de fronteira deve considerar essa particularidade do código atual.

## Registro da execução

Preencher após executar os casos no ambiente de teste:

| ID | Resultado (Passou/Falhou/Bloqueado) | Evidência / observação |
|---|---|---|
| CT-01 | A preencher | |
| CT-02 | A preencher | |
| CT-03 | A preencher | |
| CT-04 | A preencher | |
| CT-05 | A preencher | |
| CT-06 | A preencher | |
| CT-07 | A preencher | |
| CT-08 | A preencher | |
| CT-09 | A preencher | |
| CT-10 | A preencher | |
