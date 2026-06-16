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
