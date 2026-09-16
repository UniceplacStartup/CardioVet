# Plano de ação — remontagem do backend CardioVet

> Documento derivado de três insumos: o MER novo (`Veterinario_Exam_Management-2026-09-09`),
> o documento de requisitos (28 RFs, 7 RNFs, 6 histórias de usuário) e os protótipos de
> Login, Cadastro e Upload.
>
> Data: 2026-09-15

---

## 1. O que os três insumos mudam

**MER novo vs. schema atual — o delta é pequeno:**

| Tabela | Mudança |
|---|---|
| `users` | **+4 colunas**: `cpf` (UNIQUE), `phone`, `crmv` (UNIQUE), `specialty` |
| `exams` | **+1 coluna**: `report_text TEXT` — "conteúdo do laudo (Editor RICO)" |
| `tutors`, `patients`, `documents`, `document_fields` | **idênticas** ao que já está em `V1`/`V2` |

**Os requisitos, porém, pedem bem mais do que o MER comporta.** 28 RFs, 7 RNFs e 6 histórias
de usuário. E a tela de Upload revelou três coisas que o MER não tem:

- dropdown **"Modelos de Laudos"** → entidade inexistente;
- campos **"Nome do paciente"** e **"CPF do Tutor"** dentro do próprio fluxo de criar laudo;
- **geração de PDF a partir do HTML do editor** ("gerar um PDF editável", botão Download).

---

## 2. Quatro lacunas que precisam de decisão antes do código

Estas não se resolvem no código — mudam o modelo.

### D1. "Laudo" é `exams` ou entidade própria?

O MER põe `report_text` dentro de `EXAMS`. Mas produto, telas e 12 RFs (RF14–RF25) falam
exclusivamente em **laudo**, e "Meus Laudos" é um item de menu. Manter o nome `exams` no banco
e chamar de "laudo" em todo o resto garante confusão permanente entre o time.

**Recomendação:** migration de rename `exams` → `reports`, alinhando banco, entidade, API
(`/api/reports`) e linguagem do produto. O MER é artefato de design, não contrato imutável.

### D2. Não existe vínculo paciente ↔ veterinário

Este é o **gap mais sério**. RNF03 exige "cada médico tenha acesso somente aos seus próprios
dados, pacientes e laudos", RF10 diz "pacientes vinculados ao médico-veterinário" e HU06/CA01
repete. Mas `PATIENTS` no MER só tem `tutor_id` — **não há `veterinarian_id`**.

Hoje o vínculo só é derivável via `exams.veterinarian_id`, o que significa: paciente sem laudo
não pertence a ninguém, e filtrar a lista de pacientes vira um `EXISTS` caro em toda consulta.

**Recomendação:** adicionar `patients.veterinarian_id UUID NOT NULL FK → users`. Sem isso,
RNF03 e HU06/CA07 não têm como ser implementados corretamente.

### D3. Pacientes nascem do fluxo de laudo

Não existe RF de "cadastrar paciente" — só listar (RF10), pesquisar (RF11) e visualizar (RF12).
E a tela de Upload pede **nome do paciente + CPF do tutor** direto no formulário do laudo.

Leitura: **o cadastro de tutor e paciente é implícito**, um *upsert* durante a criação do laudo.
Isso tem consequência de schema — `tutors.document` (o CPF) vira **chave de busca e precisa ser
UNIQUE**, coisa que o MER não marca.

### D4. Conflito de segurança no Cenário 07

O documento diz: *"Quando informar um e-mail não cadastrado → o sistema deve informar que não foi
possível realizar a solicitação."* Isso é **user enumeration** — permite descobrir quais e-mails
existem na base, e contradiz RNF05.

**Recomendação:** resposta sempre genérica ("se o e-mail existir, as instruções foram enviadas").
Como é entrega acadêmica avaliada por critério de aceite, a escolha é do time — mas não deve ser
implementado em silêncio.

---

## 3. Arquitetura alvo

Mantém o padrão atual (Controller → Service → Repository → DTO record), reorganizado por contexto:

```
com.cardiovet
├── auth/          register, login, forgot/reset password, logout
├── user/          entidade + perfil (me)
├── profile/       GET/PUT /api/me, change-password
├── tutor/         upsert por CPF
├── patient/       listagem escopada ao vet, busca, histórico
├── report/        laudo: CRUD, editor rico, PDF
│   ├── template/  modelos de laudo
│   └── pdf/       renderização HTML → PDF
├── document/      upload do PDF + extração (motor atual)
├── security/      JWT, filtro, escopo por usuário
└── common/        ApiError, handler, validators, sanitizer
```

---

## 4. Aproveitamento do que existe

### Reaproveita integralmente (~40% do código, 0 retrabalho)

`SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`,
`GlobalExceptionHandler` + `ApiError`, `ApplicationConfig`, `OpenApiConfig`, Flyway,
`User implements UserDetails`, e o padrão de camadas inteiro.

### Reaproveita com adaptação

- **`PdfExtractionService` — continua sendo o ativo mais valioso.** O motor dirigido por
  `DEFINITIONS` sobrevive intacto; ganha um papel novo: pré-preencher o editor rico a partir
  do PDF enviado.
- `DocumentService` — a lógica de upload (valida PDF, SHA-256, extração tolerante a falha com
  status `ERRO`) continua; muda o destino dos dados.
- `PatientService` — a busca paginada vira busca **escopada ao veterinário logado**.

### Descarta

- `PatientController`/`PatientRequest` como CRUD público (vira leitura + upsert interno,
  conforme D3).
- `role` no payload de registro — hoje **qualquer um se cadastra como ADMIN** mandando
  `"role":"ADMIN"` no JSON. Furo de segurança a fechar já.

---

## 5. Fases

### Fase 1 — Identidade (RF01–RF09, RNF01, RNF02, RNF04)

**Migrations:**

- `V3__user_profile.sql`: as 4 colunas novas em `users`, com backfill antes do `NOT NULL`.
- `V4__password_reset.sql`: tabela de tokens (`token_hash`, `expires_at`, `used_at`).

**Validadores** em `common/validation`:

- `@Cpf` com dígito verificador real;
- `@StrongPassword` devolvendo **qual das 5 regras** falhou (a tela lista as cinco separadamente);
- `@FieldsMatch` para e-mail e senha (CA03/CA04 — revalidados no servidor, sem confiar no front).

**Endpoints:** `register`, `login`, `forgot-password`, `reset-password`, `logout`,
`GET /api/me`, `PUT /api/me`, `POST /api/me/change-password`.

**Dois pontos finos:**

1. O 409 precisa dizer **qual** dos três campos únicos colidiu (e-mail, CPF ou CRMV), senão a
   tela não destaca o campo certo.
2. `PUT /api/me` só aceita os 4 campos que CA02 autoriza — **nome, telefone, e-mail,
   especialidade**. CPF e CRMV são imutáveis pelo usuário.

### Fase 2 — Domínio (D1/D2/D3 aplicados)

`V5`: `patients.veterinarian_id`, `tutors.document` UNIQUE + NOT NULL, `reports.report_text`,
tabela `report_templates`.

`TutorPatientResolver`: dado CPF do tutor + nome do paciente, resolve ou cria ambos numa
transação — o coração do fluxo da tela de Upload.

**Isolamento por veterinário (RNF03)** como preocupação transversal, não `if` espalhado:
`@PreAuthorize` nos endpoints + filtro obrigatório por `veterinarian_id` na camada de
repositório. Acesso a recurso alheio devolve **404, não 403** (403 confirma que o recurso existe).

### Fase 3 — Laudo e editor rico (RF16–RF20, HU03)

`POST/PUT/GET /api/reports`, `GET /api/report-templates`.

**Sanitização obrigatória do HTML** que vem do editor — OWASP Java HTML Sanitizer ou jsoup com
whitelist restrita aos botões que a tela tem (negrito, itálico, sublinhado, tachado, alinhamento,
listas, indentação, link). Sem isso é XSS armazenado direto no banco, e o laudo é renderizado
depois para outros usuários.

### Fase 4 — PDF (RF21, RF22)

**PDFBox não gera PDF a partir de HTML** — ele só extrai. Precisa de dependência nova:
**OpenHTMLtoPDF** (Flying Saucer), que renderiza XHTML+CSS. Entra aqui o template do laudo com
cabeçalho da clínica, dados do vet (nome, CRMV — os mesmos do rodapé da sidebar), paciente e tutor.

### Fase 5 — Consultas e endurecimento (RF10–RF15, RF23–RF25, RNF06, RNF07)

Busca de laudos por paciente **e** data via `Specification` única — hoje `DocumentService.list`
ignora silenciosamente `from`/`to` quando recebe `patientId`. Histórico do paciente em ordem
cronológica decrescente (CA03).

---

## 6. Riscos técnicos

1. **`documents.content BYTEA` + `@Basic(LAZY)` sem bytecode enhancement no `pom.xml`.**
   Na prática, *toda* consulta carrega os PDFs inteiros na memória. Ameaça direta ao
   **RNF07 (3 segundos)**. O MER manteve o `bytea`; recomenda-se no mínimo isolar o blob em
   tabela própria com `@OneToOne(LAZY)`.
2. **Logout (RF06) com JWT stateless não existe de graça** — o token continua válido até expirar.
   Ou aceita-se descarte no cliente, ou entra blocklist de `jti` no Redis/banco.
3. **HTTPS (RNF06)** é configuração de deploy, não de código — precisa constar no plano de entrega.
4. **Testes:** hoje só `contextLoads`, e exige Postgres no ar. Com a remodelagem: Testcontainers
   para migrations/repositórios, e teste unitário puro de `PdfExtractionService.parse(String)` —
   o método já é package-private exatamente para isso, e é a lógica que mais dói quebrar em silêncio.
5. **Migrations `V1`/`V2` são imutáveis.** Flyway valida checksum; editá-las impede a aplicação
   de subir.

---

## 7. Decisões pendentes

| # | Decisão | Recomendação |
|---|---|---|
| D1 | Renomear `exams` → `reports` | Sim |
| D2 | Adicionar `patients.veterinarian_id` | Sim — trava RNF03 e HU06 inteira |
| D3 | `tutors.document` UNIQUE + NOT NULL | Sim |
| D4 | Resposta genérica no forgot-password | Sim, apesar do Cenário 07 |
| — | SMTP real ou stub de dev para redefinição de senha | A definir |

**D2 é a que trava mais coisa:** sem ela, RNF03 e a HU06 inteira ficam sem base.

---

## Anexo — Rastreabilidade RF → endpoint

| RF | Endpoint |
|---|---|
| RF01, RF02 | `POST /api/auth/register` |
| RF03, RF04 | `POST /api/auth/login` |
| RF05 | `POST /api/auth/forgot-password`, `POST /api/auth/reset-password` |
| RF06 | `POST /api/auth/logout` |
| RF07 | `GET /api/me` |
| RF08, RF09 | `PUT /api/me` |
| RF10, RF11 | `GET /api/patients?search=` |
| RF12 | `GET /api/patients/{id}` |
| RF13, RF14 | `GET /api/patients/{id}/reports` |
| RF15 | `GET /api/reports/{id}` |
| RF16, RF17 | `POST /api/reports` |
| RF18, RF19, RF20 | `PUT /api/reports/{id}` |
| RF21, RF22 | `GET /api/reports/{id}/pdf` |
| RF23, RF24, RF25 | `GET /api/reports?search=&from=&to=` |
| RF26, RF27, RF28 | Frontend (navegação) |
| HU03/CA01 | `GET /api/report-templates` |
