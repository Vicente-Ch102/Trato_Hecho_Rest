CREATE TABLE catalog_items (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    item_type VARCHAR(20) NOT NULL,
    default_price DECIMAL(15,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX ix_catalog_tenant (tenant_id)
) ENGINE=InnoDB;

CREATE TABLE tax_configs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(7,3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX ix_tax_tenant (tenant_id)
) ENGINE=InnoDB;

CREATE TABLE quotes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    customer_id BINARY(16) NOT NULL,
    code VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    valid_until DATE NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_quote_tenant_code UNIQUE (tenant_id, code),
    INDEX ix_quote_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE quote_lines (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    quote_id BINARY(16) NOT NULL,
    catalog_item_id BINARY(16) NULL,
    item_name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    quantity DECIMAL(12,3) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    tax_name VARCHAR(100) NULL,
    tax_rate DECIMAL(7,3) NOT NULL,
    line_net DECIMAL(15,2) NOT NULL,
    line_tax DECIMAL(15,2) NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_quote_line_quote FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
    INDEX ix_quote_lines_parent (tenant_id, quote_id)
) ENGINE=InnoDB;

CREATE TABLE quote_follow_ups (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    quote_id BINARY(16) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    note TEXT NOT NULL,
    next_contact_at DATETIME(6) NULL,
    outcome VARCHAR(300) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_followup_quote FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
    INDEX ix_followup_quote (tenant_id, quote_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE idempotency_records (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    idem_key VARCHAR(80) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_commercial_idem UNIQUE (tenant_id, idem_key, resource_type),
    INDEX ix_commercial_idem_resource (tenant_id, resource_id)
) ENGINE=InnoDB;
