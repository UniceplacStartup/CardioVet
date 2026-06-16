-- CardioVet :: schema inicial
-- Postgres 16 ja possui gen_random_uuid() nativo.

-- ============================================================
-- Usuarios (veterinarios / administradores)  -- RNF9, Auditoria
-- ============================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'VETERINARIO',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- Tutores (responsaveis pelo animal)
-- ============================================================
CREATE TABLE tutors (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(180),
    phone      VARCHAR(30),
    document   VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- Pacientes (animais)
-- ============================================================
CREATE TABLE patients (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tutor_id   UUID         NOT NULL REFERENCES tutors (id),
    name       VARCHAR(120) NOT NULL,
    species    VARCHAR(60)  NOT NULL,
    breed      VARCHAR(80),
    sex        VARCHAR(10),
    birth_date DATE,
    weight_kg  NUMERIC(6,2),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_patients_tutor ON patients (tutor_id);

-- ============================================================
-- Exames de ecocardiografia
-- ============================================================
CREATE TABLE exams (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID         NOT NULL REFERENCES patients (id),
    veterinarian_id UUID         NOT NULL REFERENCES users (id),
    exam_date       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status          VARCHAR(30)  NOT NULL DEFAULT 'EM_ANDAMENTO',
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_exams_patient ON exams (patient_id);
CREATE INDEX idx_exams_vet ON exams (veterinarian_id);
