CREATE TABLE tenants (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE user_accounts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    INDEX ix_user_tenant (tenant_id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BINARY(16) NOT NULL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL,
    user_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    INDEX ix_refresh_user (user_id)
) ENGINE=InnoDB;
