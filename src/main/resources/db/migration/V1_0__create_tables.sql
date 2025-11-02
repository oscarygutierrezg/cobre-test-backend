-- Crear esquema si no existe
CREATE SCHEMA IF NOT EXISTS cbmm;

-- Crear secuencia para revinfo (requerida por Hibernate Envers)
CREATE SEQUENCE IF NOT EXISTS cbmm.revinfo_seq START WITH 1 INCREMENT BY 50;

-- Tabla de información de revisión para auditoría
CREATE TABLE cbmm.revinfo (
                              rev      INTEGER PRIMARY KEY DEFAULT nextval('cbmm.revinfo_seq'),
                              revtstmp BIGINT
);

-- Tabla de cuentas
CREATE TABLE cbmm.account (
                              account_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              account_number  VARCHAR(50) NOT NULL UNIQUE,
                              currency        VARCHAR(10) NOT NULL,
                              balance         DECIMAL(18,2) NOT NULL DEFAULT 0,
                              status          VARCHAR(50) NOT NULL,
                              created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              version         INTEGER NOT NULL DEFAULT 0
);

-- Tabla de transacciones
CREATE TABLE cbmm.transaction (
                                  transaction_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  account_id      UUID NOT NULL,
                                  amount          DECIMAL(18,2) NOT NULL,
                                  type            VARCHAR(50) NOT NULL,
                                  currency        VARCHAR(10) NOT NULL,
                                  balance_after   DECIMAL(18,2) NOT NULL,
                                  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  status          VARCHAR(50) NOT NULL,
                                  CONSTRAINT fk_transaction_account FOREIGN KEY (account_id)
                                      REFERENCES cbmm.account(account_id)
);

-- Tabla de eventos CBMM
CREATE TABLE cbmm.cbmm_event (
                                 event_id           VARCHAR(100) PRIMARY KEY,
                                 event_type         VARCHAR(100) NOT NULL,
                                 operation_date     TIMESTAMP NOT NULL,
                                 origin_data        JSONB NOT NULL,
                                 destination_data   JSONB NOT NULL,
                                 status             VARCHAR(50) NOT NULL,
                                 created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 retry_count        INTEGER NOT NULL DEFAULT 0,
                                 version            INTEGER NOT NULL DEFAULT 0
);

-- Tabla de auditoría de cuentas
CREATE TABLE cbmm.account_aud (
    account_id      UUID NOT NULL,
    rev             INTEGER NOT NULL,
    revtype         SMALLINT,
    account_number  VARCHAR(50),
    currency        VARCHAR(10),
    balance         DECIMAL(18,2),
    status          VARCHAR(50),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    version         INTEGER,
    PRIMARY KEY (rev, account_id),
    CONSTRAINT fk_account_aud_rev FOREIGN KEY (rev) REFERENCES cbmm.revinfo(rev)
);

-- Tabla de auditoría de transacciones
CREATE TABLE cbmm.transaction_aud (
    transaction_id  UUID NOT NULL,
    rev             INTEGER NOT NULL,
    revtype         SMALLINT,
    account_id      UUID,
    amount          DECIMAL(18,2),
    type            VARCHAR(50),
    currency        VARCHAR(10),
    balance_after   DECIMAL(18,2),
    created_at      TIMESTAMP,
    status          VARCHAR(50),
    PRIMARY KEY (rev, transaction_id),
    CONSTRAINT fk_transaction_aud_rev FOREIGN KEY (rev) REFERENCES cbmm.revinfo(rev)
);

-- Tabla de auditoría de eventos CBMM
CREATE TABLE cbmm.cbmm_event_aud (
    event_id           VARCHAR(100) NOT NULL,
    rev                INTEGER NOT NULL,
    revtype            SMALLINT,
    event_type         VARCHAR(100),
    operation_date     TIMESTAMP,
    origin_data        JSONB,
    destination_data   JSONB,
    status             VARCHAR(50),
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    retry_count        INTEGER,
    version            INTEGER,
    PRIMARY KEY (rev, event_id),
    CONSTRAINT fk_cbmm_event_aud_rev FOREIGN KEY (rev) REFERENCES cbmm.revinfo(rev)
);

-- Índices para mejorar el rendimiento
CREATE INDEX idx_account_number ON cbmm.account(account_number);
CREATE INDEX idx_transaction_account_id ON cbmm.transaction(account_id);
CREATE INDEX idx_transaction_created_at ON cbmm.transaction(created_at);
CREATE INDEX idx_account_status ON cbmm.account(status);
CREATE INDEX idx_account_aud_rev ON cbmm.account_aud(rev);
CREATE INDEX idx_transaction_aud_rev ON cbmm.transaction_aud(rev);
CREATE INDEX idx_cbmm_event_status ON cbmm.cbmm_event(status);
CREATE INDEX idx_cbmm_event_operation_date ON cbmm.cbmm_event(operation_date);
CREATE INDEX idx_cbmm_event_created_at ON cbmm.cbmm_event(created_at);
CREATE INDEX idx_cbmm_event_aud_rev ON cbmm.cbmm_event_aud(rev);
