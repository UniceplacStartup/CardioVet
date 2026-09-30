ALTER TABLE users
    ADD COLUMN cpf       VARCHAR(14),
    ADD COLUMN phone     VARCHAR(20),
    ADD COLUMN crmv      VARCHAR(30),
    ADD COLUMN specialty VARCHAR(100);

CREATE UNIQUE INDEX uq_tutors_document ON tutors (document) WHERE document IS NOT NULL;
CREATE INDEX idx_patients_name ON patients (lower(name));

ALTER TABLE documents
    ADD COLUMN report_model      VARCHAR(100),
    ADD COLUMN exam_date         DATE,
    ADD COLUMN patient_age       VARCHAR(40),
    ADD COLUMN veterinarian_name VARCHAR(150),
    ADD COLUMN findings          TEXT,
    ADD COLUMN conclusion        TEXT;

CREATE TABLE document_images (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT       NOT NULL,
    content         BYTEA        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_images_document ON document_images (document_id);

CREATE TABLE password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
