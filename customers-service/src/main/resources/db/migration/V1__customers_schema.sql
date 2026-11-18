CREATE TABLE customers (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    business_name VARCHAR(180) NOT NULL,
    rut VARCHAR(15) NOT NULL,
    contact_name VARCHAR(120) NULL,
    email VARCHAR(190) NULL,
    phone VARCHAR(30) NULL,
    address VARCHAR(250) NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_customer_tenant_rut UNIQUE (tenant_id, rut),
    INDEX ix_customer_tenant (tenant_id),
    INDEX ix_customer_business_name (business_name)
) ENGINE=InnoDB;

CREATE TABLE idempotency_records (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    idem_key VARCHAR(80) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_customer_idem UNIQUE (tenant_id, idem_key, resource_type),
    INDEX ix_customer_idem_resource (tenant_id, resource_id)
) ENGINE=InnoDB;
