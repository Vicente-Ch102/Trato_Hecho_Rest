CREATE TABLE invoices (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    quote_id BINARY(16) NOT NULL,
    folio VARCHAR(40) NOT NULL,
    issued_date DATE NOT NULL,
    due_date DATE NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_invoice_tenant_folio UNIQUE (tenant_id, folio),
    INDEX ix_invoice_tenant_issued (tenant_id, issued_date)
) ENGINE=InnoDB;
