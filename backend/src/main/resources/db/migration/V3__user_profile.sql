ALTER TABLE users
    ADD COLUMN cpf       VARCHAR(11),
    ADD COLUMN phone     VARCHAR(11),
    ADD COLUMN crmv      VARCHAR(20),
    ADD COLUMN specialty VARCHAR(100);

UPDATE users u
SET cpf   = lpad(t.rn::text, 11, '0'),
    crmv  = 'PENDENTE-' || lpad(t.rn::text, 6, '0'),
    phone = '00000000000'
FROM (SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM users) t
WHERE u.id = t.id
  AND u.cpf IS NULL;

UPDATE users SET email = lower(email) WHERE email <> lower(email);

ALTER TABLE users
    ALTER COLUMN cpf   SET NOT NULL,
    ALTER COLUMN phone SET NOT NULL,
    ALTER COLUMN crmv  SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uq_users_cpf  UNIQUE (cpf),
    ADD CONSTRAINT uq_users_crmv UNIQUE (crmv);
