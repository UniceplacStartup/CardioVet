# CardioVet

Software web para uma máquina de ecocardiografia veterinária. Permite **enviar
laudos em PDF** (com layout padrão) e **extrair automaticamente as informações**
(dados do paciente e medidas do exame), salvando os documentos por data.

## Stack

- **Backend:** Spring Boot 3.5 (Java 21), Spring Security + JWT, JPA/Hibernate,
  Flyway, Apache PDFBox, PostgreSQL 16.
- **Frontend:** Vue 3 + Vite + TypeScript, Pinia, Vue Router, Tailwind + shadcn-vue.
- **Banco:** PostgreSQL (via `docker-compose.yml`).

## Como rodar

```bash
# 1. Banco
docker compose up -d

# 2. Backend (precisa de JAVA_HOME apontando para o Java 21)
cd backend
./mvnw spring-boot:run        # http://localhost:8080  (Swagger em /swagger-ui.html)

# 3. Frontend
cd frontend
npm install
npm run dev                   # http://localhost:5173
```

Se o backend rodar em outra porta (`SERVER_PORT=8091 ./mvnw spring-boot:run`),
aponte o proxy do Vite para ela: `API_TARGET=http://localhost:8091 npm run dev`.

**Recuperação de senha:** a tela "Esqueci a senha" pede e-mail + CPF cadastrados;
se conferirem, abre direto a tela de nova senha (token de uso único, 30 min).
O envio do link por e-mail (`/auth/forgot-password`) ainda não tem SMTP — para
depurar, `PASSWORD_RESET_LOG_LINK=true` imprime o link no log (nunca em produção). A base do link vem
de `FRONTEND_URL` (padrão `http://localhost:5173`) e a validade de
`PASSWORD_RESET_TTL_MINUTES` (padrão 30).

## Funcionalidade de extração de PDF

1. Em **Documentos**, envie um PDF de laudo.
2. O backend salva o arquivo original, extrai o texto (PDFBox) e reconhece os
   campos do layout padrão (`PdfExtractionService`): identificação do paciente,
   medidas Modo-M/2D (AO, LA, IVSd/s, LVIDd/s, LVPWd/s), cálculos (FS, EF, LA/AO)
   e Doppler (ondas E/A, velocidades aórtica e pulmonar).
3. Os campos ficam disponíveis na interface, agrupados por categoria, e os
   documentos são listados **por data**.

Para suportar um novo modelo de laudo, basta acrescentar definições em
`PdfExtractionService.DEFINITIONS` — a lógica de parsing é dirigida por configuração.

## Modelo de dados (MER)

Diagrama entidade-relacionamento e DDL: [`docs/MER.md`](docs/MER.md).
Tabelas principais: `users` (e-mail institucional + senha criptografada com BCrypt),
`tutors`, `patients`, `exams`, `documents` (salvos por data) e `document_fields`
(informações extraídas).
