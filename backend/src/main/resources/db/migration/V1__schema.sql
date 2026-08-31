-- ---------------------------------------------------------------------------
-- Customer Activity Analytics - schema
--
-- The activity/risk tables follow the assignment specification verbatim
-- (transactions + card/payment/crypto subtype tables, risk_rules,
-- risk_assessments). Supporting tables: customers (referenced by FK in the
-- spec), operators (login), policy_documents/policy_chunks (RAG corpus) and
-- ai_analyses (persisted AI analysis results).
--
-- SQL is kept portable between H2 (PostgreSQL mode) and PostgreSQL:
-- enums are VARCHAR + CHECK constraints, JSON payloads are TEXT.
-- ---------------------------------------------------------------------------

CREATE TABLE customers (
    customer_id   UUID PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    segment       VARCHAR(20)  NOT NULL,      -- RETAIL / PREMIUM / BUSINESS
    country       CHAR(2)      NOT NULL,      -- customer residence country
    created_at    TIMESTAMP    NOT NULL
);

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    customer_id    UUID           NOT NULL REFERENCES customers (customer_id),
    activity_type  VARCHAR(10)    NOT NULL CHECK (activity_type IN ('CARD', 'PAYMENT', 'CRYPTO')),
    amount         DECIMAL(18, 2) NOT NULL,
    currency       VARCHAR(10)    NOT NULL,   -- ISO currency code or crypto ticker
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('COMPLETED', 'PENDING', 'FAILED', 'REVERSED')),
    created_at     TIMESTAMP      NOT NULL
);

CREATE INDEX idx_transactions_customer ON transactions (customer_id, created_at);

CREATE TABLE card_activity (
    transaction_id     UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    card_pan           VARCHAR(20)  NOT NULL,  -- masked PAN, e.g. ****1234
    card_type          VARCHAR(10)  NOT NULL,  -- Debit / Credit / Prepaid
    merchant_name      VARCHAR(120) NOT NULL,
    mcc_code           VARCHAR(4)   NOT NULL,
    card_present       BOOLEAN      NOT NULL,
    authorization_code VARCHAR(12),
    decline_reason     VARCHAR(120)
);

CREATE TABLE payment_activity (
    transaction_id        UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    payment_method        VARCHAR(10) NOT NULL,  -- ACH / WIRE / SWIFT / P2P / SEPA
    sender_account        VARCHAR(34) NOT NULL,
    receiver_account      VARCHAR(34) NOT NULL,
    receiver_bank_country CHAR(2)     NOT NULL
);

CREATE TABLE crypto_activity (
    transaction_id      UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    blockchain          VARCHAR(10) NOT NULL,   -- BTC / ETH / ...
    wallet_address_from VARCHAR(64) NOT NULL,
    wallet_address_to   VARCHAR(64) NOT NULL,
    tx_hash             VARCHAR(80) NOT NULL,
    exchange_name       VARCHAR(60)
);

CREATE TABLE risk_rules (
    rule_id         UUID PRIMARY KEY,
    rule_name       VARCHAR(120)  NOT NULL,
    applies_to      VARCHAR(10)   NOT NULL CHECK (applies_to IN ('CARD', 'PAYMENT', 'CRYPTO', 'ALL')),
    threshold_logic TEXT          NOT NULL,
    weight          DECIMAL(5, 2) NOT NULL
);

CREATE TABLE risk_assessments (
    assessment_id      UUID PRIMARY KEY,
    transaction_id     UUID          NOT NULL REFERENCES transactions (transaction_id),
    rule_id            UUID          NOT NULL REFERENCES risk_rules (rule_id),
    triggered_at       TIMESTAMP     NOT NULL,
    score_contribution DECIMAL(5, 2) NOT NULL
);

CREATE INDEX idx_risk_assessments_tx ON risk_assessments (transaction_id);

-- --------------------------------------------------------------------------
-- Operators (login)
-- --------------------------------------------------------------------------

CREATE TABLE operators (
    operator_id   UUID PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,       -- BCrypt
    display_name  VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,       -- OPERATOR / SUPERVISOR
    created_at    TIMESTAMP    NOT NULL
);

-- --------------------------------------------------------------------------
-- RAG corpus: unstructured policy/knowledge documents, chunked for retrieval.
-- Content is loaded at startup from classpath:policies/*.md (idempotent,
-- hash-based) so the markdown files remain the single source of truth.
-- --------------------------------------------------------------------------

CREATE TABLE policy_documents (
    document_id    UUID PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    source_file    VARCHAR(200) NOT NULL UNIQUE,
    content_sha256 VARCHAR(64)  NOT NULL,
    loaded_at      TIMESTAMP    NOT NULL
);

CREATE TABLE policy_chunks (
    chunk_id      UUID PRIMARY KEY,
    document_id   UUID         NOT NULL REFERENCES policy_documents (document_id),
    chunk_index   INT          NOT NULL,
    section_title VARCHAR(200),
    content       TEXT         NOT NULL
);

CREATE INDEX idx_policy_chunks_doc ON policy_chunks (document_id, chunk_index);

-- --------------------------------------------------------------------------
-- Persisted AI analysis results (auditable, reviewable later).
-- --------------------------------------------------------------------------

CREATE TABLE ai_analyses (
    analysis_id            UUID PRIMARY KEY,
    customer_id            UUID        NOT NULL REFERENCES customers (customer_id),
    operator_id            UUID        NOT NULL REFERENCES operators (operator_id),
    created_at             TIMESTAMP   NOT NULL,
    llm_mode               VARCHAR(12) NOT NULL,  -- ANTHROPIC / STUB
    model                  VARCHAR(60) NOT NULL,
    risk_level             VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    summary                TEXT        NOT NULL,
    findings_json          TEXT        NOT NULL,  -- [{title, severity, evidence, policyRefs[]}]
    recommendations_json   TEXT        NOT NULL,  -- [string]
    salient_signals_json   TEXT        NOT NULL,  -- output of the triage LLM call
    retrieval_queries_json TEXT        NOT NULL,  -- queries used against the policy corpus
    retrieved_chunks_json  TEXT        NOT NULL,  -- [{documentTitle, sectionTitle, excerpt}]
    input_tokens           BIGINT,
    output_tokens          BIGINT,
    latency_ms             BIGINT      NOT NULL
);

CREATE INDEX idx_ai_analyses_customer ON ai_analyses (customer_id, created_at);
