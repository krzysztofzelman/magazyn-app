CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL DEFAULT 'free',
    max_users INTEGER NOT NULL DEFAULT 3,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Default tenant for existing single-tenant deployment
INSERT INTO tenants (subdomain, name, api_key, plan, max_users)
VALUES ('default', 'Default Warehouse', gen_random_uuid()::text, 'self-hosted', 100);
