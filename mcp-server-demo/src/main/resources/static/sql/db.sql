-- ─────────────────────────────────────────────────────────────────────────────
-- MCP Demo Schema
-- Run once against your MySQL instance:
--   mysql -u root -p < schema.sql
-- ─────────────────────────────────────────────────────────────────────────────

CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;

-- ── employees ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
                                         id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(100),
    salary     DECIMAL(12, 2),
    hire_date  DATE,
    active     BOOLEAN DEFAULT TRUE
    );

-- ── products ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
                                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        name        VARCHAR(200) NOT NULL,
    category    VARCHAR(100),
    price       DECIMAL(10, 2) NOT NULL,
    stock_qty   INT DEFAULT 0,
    sku         VARCHAR(80) UNIQUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ── orders ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
                                      id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      employee_id BIGINT,
                                      product_id  BIGINT,
                                      quantity    INT NOT NULL,
                                      total_price DECIMAL(12, 2),
    order_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status      ENUM('PENDING','PROCESSING','SHIPPED','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (product_id)  REFERENCES products(id)
    );

-- ── Seed data ─────────────────────────────────────────────────────────────────
INSERT INTO employees (first_name, last_name, email, department, salary, hire_date) VALUES
                                                                                        ('Alice',   'Johnson',  'alice@example.com',   'Engineering',  95000.00, '2021-03-15'),
                                                                                        ('Bob',     'Smith',    'bob@example.com',     'Marketing',    72000.00, '2020-07-01'),
                                                                                        ('Carol',   'Williams', 'carol@example.com',   'Engineering', 105000.00, '2019-11-20'),
                                                                                        ('David',   'Brown',    'david@example.com',   'HR',           68000.00, '2022-01-10'),
                                                                                        ('Eva',     'Davis',    'eva@example.com',     'Engineering',  98000.00, '2023-06-05'),
                                                                                        ('Frank',   'Miller',   'frank@example.com',   'Sales',        81000.00, '2020-09-14'),
                                                                                        ('Grace',   'Wilson',   'grace@example.com',   'Marketing',    76000.00, '2021-12-01'),
                                                                                        ('Henry',   'Moore',    'henry@example.com',   'Sales',        79000.00, '2022-04-18');

INSERT INTO products (name, category, price, stock_qty, sku) VALUES
                                                                 ('Laptop Pro 15',      'Electronics',  1299.99,  45, 'ELEC-LP15'),
                                                                 ('Wireless Mouse',     'Electronics',    29.99, 200, 'ELEC-WM01'),
                                                                 ('Standing Desk',      'Furniture',     499.00,  12, 'FURN-SD01'),
                                                                 ('Ergonomic Chair',    'Furniture',     349.00,  30, 'FURN-EC01'),
                                                                 ('USB-C Hub 7-in-1',   'Electronics',    59.99, 150, 'ELEC-HUB7'),
                                                                 ('Notebook Pack (5)',  'Stationery',      9.99, 500, 'STAT-NB05'),
                                                                 ('Monitor 27" 4K',     'Electronics',   799.00,  20, 'ELEC-M27K'),
                                                                 ('Desk Lamp LED',      'Furniture',      45.00,  80, 'FURN-DL01');

INSERT INTO orders (employee_id, product_id, quantity, total_price, status) VALUES
                                                                                (1, 1, 1, 1299.99, 'DELIVERED'),
                                                                                (1, 2, 2,   59.98, 'DELIVERED'),
                                                                                (2, 5, 1,   59.99, 'SHIPPED'),
                                                                                (3, 7, 2, 1598.00, 'PROCESSING'),
                                                                                (4, 6, 5,   49.95, 'DELIVERED'),
                                                                                (5, 3, 1,  499.00, 'PENDING'),
                                                                                (6, 4, 2,  698.00, 'SHIPPED'),
                                                                                (7, 8, 3,  135.00, 'DELIVERED'),
                                                                                (8, 2, 4,  119.96, 'PENDING'),
                                                                                (1, 5, 1,   59.99, 'PROCESSING');
