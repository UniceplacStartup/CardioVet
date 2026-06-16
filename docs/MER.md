# CardioVet — Modelo Entidade-Relacionamento (MER)

Banco: **PostgreSQL 16**. Chaves primárias em `UUID` (`gen_random_uuid()`),
carimbos de tempo em `TIMESTAMPTZ`. O esquema é versionado por Flyway
(`backend/src/main/resources/db/migration`).

O diagrama abaixo está em **Mermaid** (`erDiagram`) — renderiza no GitHub, no
VS Code (extensão Mermaid) e em https://mermaid.live.

```mermaid
erDiagram
    USERS ||--o{ EXAMS : "realiza (veterinario)"
    USERS ||--o{ DOCUMENTS : "envia"
    TUTORS ||--o{ PATIENTS : "possui"
    PATIENTS ||--o{ EXAMS : "e examinado em"
    PATIENTS ||--o{ DOCUMENTS : "tem laudo"
    EXAMS ||--o{ DOCUMENTS : "gera"
    DOCUMENTS ||--o{ DOCUMENT_FIELDS : "extrai"

    USERS {
        uuid        id PK
        varchar     name
        varchar     email "UNIQUE - e-mail institucional"
        varchar     password_hash "senha criptografada (BCrypt)"
        varchar     role "ADMIN | VETERINARIO"
        boolean     active
        timestamptz created_at
        timestamptz updated_at
    }

    TUTORS {
        uuid        id PK
        varchar     name
        varchar     email
        varchar     phone
        varchar     document "CPF/RG"
        timestamptz created_at
    }

    PATIENTS {
        uuid        id PK
        uuid        tutor_id FK
        varchar     name
        varchar     species
        varchar     breed
        varchar     sex
        date        birth_date
        numeric     weight_kg
        timestamptz created_at
    }

    EXAMS {
        uuid        id PK
        uuid        patient_id FK
        uuid        veterinarian_id FK
        timestamptz exam_date
        varchar     status "EM_ANDAMENTO | FINALIZADO | CANCELADO"
        text        notes
        timestamptz created_at
        timestamptz updated_at
    }

    DOCUMENTS {
        uuid        id PK
        uuid        exam_id FK "opcional"
        uuid        patient_id FK "opcional"
        uuid        uploaded_by FK
        varchar     file_name
        varchar     content_type
        bigint      file_size_bytes
        varchar     sha256
        bytea       content "PDF original salvo"
        date        document_date "salvos por data"
        varchar     status "PENDENTE | PROCESSADO | ERRO"
        text        extracted_text
        text        error_message
        timestamptz created_at
        timestamptz updated_at
    }

    DOCUMENT_FIELDS {
        uuid        id PK
        uuid        document_id FK
        varchar     field_key "ex: LVIDd"
        varchar     label
        varchar     value
        varchar     unit "mm | % | bpm | m/s"
        varchar     category "PACIENTE | MODO_M | DOPPLER | CALCULO"
        timestamptz created_at
    }
```

## Relacionamentos (cardinalidade)

| De | Para | Cardinalidade | Regra |
|----|------|---------------|-------|
| `tutors` | `patients` | 1 : N | Um tutor possui vários animais. |
| `patients` | `exams` | 1 : N | Um paciente realiza vários exames. |
| `users` | `exams` | 1 : N | O veterinário responsável pelo exame. |
| `users` | `documents` | 1 : N | Quem enviou o PDF (`uploaded_by`). |
| `patients` | `documents` | 1 : N | Laudos vinculados a um animal (opcional). |
| `exams` | `documents` | 1 : N | PDF gerado por um exame (opcional). |
| `documents` | `document_fields` | 1 : N | Pares rótulo/valor extraídos do PDF. |

## Tabelas centrais do requisito

- **`users`** — tabela padrão de usuários: `name`, `email` institucional (único),
  `password_hash` (senha **criptografada com BCrypt** — ver `ApplicationConfig`/`AuthService`),
  `role`, `active` e auditoria (`created_at`/`updated_at`).
- **`documents`** — documentos salvos **por data** (`document_date`, indexada).
  Guarda o PDF original (`content` BYTEA), metadados, status de extração e o texto bruto.
- **`document_fields`** — informações **extraídas** do PDF no layout padrão
  (medidas de ecocardiografia), normalizadas em `field_key`/`value`/`unit`/`category`.

## DDL de referência

O esquema real é criado pelas migrações Flyway:

- `V1__init_schema.sql` — `users`, `tutors`, `patients`, `exams`.
- `V2__documents.sql` — `documents`, `document_fields`.
