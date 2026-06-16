-- Integration test seed data
-- Passwords are BCrypt hashes of 'password123' — only needed for /login endpoint tests

INSERT INTO tenants (id, subdomain, name, api_key, plan, max_users, is_active, created_at)
VALUES
  (1, 'default',  'Default Tenant',  'test-api-key-default',  'self-hosted', 100, true, NOW()),
  (2, 'other',    'Other Tenant',    'test-api-key-other',    'self-hosted', 100, true, NOW());

ALTER SEQUENCE tenants_id_seq RESTART WITH 3;

INSERT INTO warehouses (id, name, code, is_active, tenant_id, created_at)
VALUES (1, 'Main Warehouse', 'MAIN', true, 1, NOW());

ALTER SEQUENCE warehouses_id_seq RESTART WITH 2;

INSERT INTO users (id, username, password, role, is_active, tenant_id, created_at, updated_at)
VALUES
  (1, 'admin',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN',     true, 1, NOW(), NOW()),
  (2, 'user',        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_USER',      true, 1, NOW(), NOW()),
  (3, 'testuser',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_WAREHOUSE', true, 1, NOW(), NOW()),
  (4, 'regularuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_WAREHOUSE', true, 1, NOW(), NOW()),
  (5, 'adminuser',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN',     true, 1, NOW(), NOW()),
  (6, 'other',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN',     true, 2, NOW(), NOW());

ALTER SEQUENCE users_id_seq RESTART WITH 7;
