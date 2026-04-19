-- MCP Demo Test Database Setup
-- Run this script once to prepare the test database

CREATE DATABASE IF NOT EXISTS mcp_demo_test;
USE mcp_demo_test;

-- Create tables (same structure as production)
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

-- Note: Tables will be auto-created by Hibernate during tests via ddl-auto: create-drop
-- This script is just for reference

