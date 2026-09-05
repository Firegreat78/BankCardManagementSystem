CREATE TABLE users (
    id       VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE card (
    id               VARCHAR(255)   NOT NULL,
    number           VARCHAR(255)   NOT NULL,
    number_hash      VARCHAR(255)   NOT NULL,
    holder_id        VARCHAR(255)   NOT NULL,
    status           VARCHAR(255)   NOT NULL,
    balance          NUMERIC(38, 2) NOT NULL,
    expiration_date  DATE           NOT NULL,
    CONSTRAINT pk_card PRIMARY KEY (id),
    CONSTRAINT uq_card_number_hash UNIQUE (number_hash),
    CONSTRAINT fk_card_holder FOREIGN KEY (holder_id) REFERENCES users (id),
    CONSTRAINT chk_card_status CHECK (status IN ('ACTIVE', 'BLOCK_REQUESTED', 'BLOCKED', 'UNBLOCK_REQUESTED', 'EXPIRED'))
);

CREATE INDEX idx_card_holder_id ON card (holder_id);
