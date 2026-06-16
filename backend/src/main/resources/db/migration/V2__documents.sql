-- CardioVet :: documentos (laudos em PDF) e campos extraidos
-- Suporta o requisito de "extracao de informacoes de um documento PDF" e
-- o armazenamento dos "documentos salvos por data".

-- ============================================================
-- Documentos PDF enviados (laudos de ecocardiografia)
-- ============================================================
CREATE TABLE documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Vinculos opcionais: o PDF pode ser enviado antes de associar paciente/exame.
    exam_id         UUID         REFERENCES exams (id)     ON DELETE SET NULL,
    patient_id      UUID         REFERENCES patients (id)  ON DELETE SET NULL,
    uploaded_by     UUID         NOT NULL REFERENCES users (id),

    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    file_size_bytes BIGINT       NOT NULL,
    sha256          VARCHAR(64),
    content         BYTEA        NOT NULL,                  -- PDF original salvo

    -- Data do documento (extraida do laudo ou informada no upload) -> "salvos por data".
    document_date   DATE,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDENTE', -- PENDENTE / PROCESSADO / ERRO
    extracted_text  TEXT,                                   -- texto bruto extraido do PDF
    error_message   TEXT,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_date        ON documents (document_date DESC);
CREATE INDEX idx_documents_patient     ON documents (patient_id);
CREATE INDEX idx_documents_exam        ON documents (exam_id);
CREATE INDEX idx_documents_uploaded_by ON documents (uploaded_by);

-- ============================================================
-- Campos extraidos do PDF (pares rotulo/valor do layout padrao)
-- ============================================================
CREATE TABLE document_fields (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    field_key   VARCHAR(80)  NOT NULL,   -- chave normalizada, ex: LVIDd
    label       VARCHAR(150),            -- rotulo legivel, ex: Diametro interno VE (diastole)
    value       VARCHAR(255),
    unit        VARCHAR(30),             -- mm, %, bpm, cm/s...
    category    VARCHAR(60),             -- PACIENTE / MODO_M / DOPPLER / CALCULO
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_fields_document ON document_fields (document_id);
CREATE UNIQUE INDEX uq_document_fields_doc_key ON document_fields (document_id, field_key);
