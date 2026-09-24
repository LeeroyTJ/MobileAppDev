-- ============================================================
-- CohortHub Database Schema
-- ICT361 Mobile Application Development
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS cohorthub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cohorthub

-- ============================================================
-- 1. ACCOUNTS
-- ============================================================

CREATE TABLE accounts (
    account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    username VARCHAR(100) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    role ENUM('STUDENT', 'LECTURER') NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (account_id),

    UNIQUE KEY uk_accounts_username (username)
) ENGINE = InnoDB;


-- ============================================================
-- 2. PROGRAMMES
-- ============================================================

CREATE TABLE programmes (
    programme_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    code VARCHAR(20) NOT NULL,

    name VARCHAR(150) NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (programme_id),

    UNIQUE KEY uk_programmes_code (code),

    UNIQUE KEY uk_programmes_name (name)
) ENGINE = InnoDB;


-- ============================================================
-- 3. LAB GROUPS
-- ============================================================

CREATE TABLE groups (
    group_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    group_code VARCHAR(20) NOT NULL,

    name VARCHAR(100) NOT NULL,

    capacity INT UNSIGNED NOT NULL DEFAULT 15,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (group_id),

    UNIQUE KEY uk_groups_code (group_code),

    CONSTRAINT chk_groups_capacity
        CHECK (capacity > 0 AND capacity <= 15)
) ENGINE = InnoDB;


-- ============================================================
-- 4. STUDENTS
-- ============================================================

CREATE TABLE students (
    student_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    account_id BIGINT UNSIGNED NULL,

    programme_id BIGINT UNSIGNED NOT NULL,

    group_id BIGINT UNSIGNED NULL,

    student_number VARCHAR(50) NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(255) NULL,

    phone VARCHAR(30) NULL,

    -- Used for optimistic concurrency/conflict detection.
    version INT UNSIGNED NOT NULL DEFAULT 1,

    -- NULL = active.
    -- Non-NULL = soft deleted.
    deleted_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (student_id),

    UNIQUE KEY uk_students_student_number (student_number),

    UNIQUE KEY uk_students_account (account_id),

    KEY idx_students_programme (programme_id),

    KEY idx_students_group (group_id),

    KEY idx_students_name (last_name, first_name),

    CONSTRAINT fk_students_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_students_programme
        FOREIGN KEY (programme_id)
        REFERENCES programmes(programme_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_students_group
        FOREIGN KEY (group_id)
        REFERENCES groups(group_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- ============================================================
-- 5. CLAIM CODES
-- ============================================================

CREATE TABLE claim_codes (
    claim_code_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    programme_id BIGINT UNSIGNED NULL,

    code_hash VARCHAR(255) NOT NULL,

    expires_at DATETIME NOT NULL,

    used_at DATETIME NULL,

    used_by_account_id BIGINT UNSIGNED NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (claim_code_id),

    UNIQUE KEY uk_claim_codes_hash (code_hash),

    KEY idx_claim_codes_programme (programme_id),

    KEY idx_claim_codes_used_by (used_by_account_id),

    CONSTRAINT fk_claim_codes_programme
        FOREIGN KEY (programme_id)
        REFERENCES programmes(programme_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_claim_codes_account
        FOREIGN KEY (used_by_account_id)
        REFERENCES accounts(account_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- ============================================================
-- 6. SYNC OPERATIONS
--
-- This table is included to support the later sync/idempotency
-- requirements. It allows the backend to remember an operation
-- that has already been processed.
-- ============================================================

CREATE TABLE sync_operations (
    operation_id CHAR(36) NOT NULL,

    account_id BIGINT UNSIGNED NOT NULL,

    operation_type VARCHAR(50) NOT NULL,

    entity_type VARCHAR(50) NOT NULL,

    entity_id BIGINT UNSIGNED NULL,

    status ENUM(
        'PROCESSING',
        'APPLIED',
        'CONFLICT',
        'REJECTED'
    ) NOT NULL DEFAULT 'PROCESSING',

    request_hash VARCHAR(64) NULL,

    response_payload JSON NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at DATETIME NULL,

    PRIMARY KEY (operation_id),

    KEY idx_sync_operations_account (account_id),

    KEY idx_sync_operations_entity (
        entity_type,
        entity_id
    ),

    CONSTRAINT fk_sync_operations_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- ============================================================
-- 7. SEED PROGRAMMES
-- ============================================================

INSERT INTO programmes (code, name)
VALUES
    ('CS', 'Computer Science'),
    ('SE', 'Software Engineering'),
    ('IT', 'Information Technology');


-- ============================================================
-- 8. SEED LAB GROUPS
-- ============================================================

INSERT INTO groups (group_code, name, capacity)
VALUES
    ('G01', 'Lab Group 01', 15),
    ('G02', 'Lab Group 02', 15),
    ('G03', 'Lab Group 03', 15);


-- ============================================================
-- 9. OPTIONAL TEST LECTURER
--
-- IMPORTANT:
-- Replace the password hash with a real bcrypt/Argon2 hash
-- generated by the backend.
-- Never put plaintext passwords into this file.
-- ============================================================

-- Example only:
--
-- INSERT INTO accounts (
--     username,
--     password_hash,
--     role
-- )
-- VALUES (
--     'lecturer01',
--     '<BCRYPT_HASH_HERE>',
--     'LECTURER'
-- );