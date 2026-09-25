erDiagram

    ACCOUNTS {
        BIGINT account_id PK
        VARCHAR username UK
        VARCHAR password_hash
        ENUM role
        BOOLEAN is_active
        DATETIME created_at
        DATETIME updated_at
    }

    PROGRAMMES {
        BIGINT programme_id PK
        VARCHAR code UK
        VARCHAR name UK
        BOOLEAN is_active
        DATETIME created_at
        DATETIME updated_at
    }

    GROUPS {
        BIGINT group_id PK
        VARCHAR group_code UK
        VARCHAR name
        INT capacity
        BOOLEAN is_active
        DATETIME created_at
        DATETIME updated_at
    }

    STUDENTS {
        BIGINT student_id PK
        BIGINT account_id FK
        BIGINT programme_id FK
        BIGINT group_id FK
        VARCHAR student_number UK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email
        VARCHAR phone
        INT version
        DATETIME deleted_at
        DATETIME created_at
        DATETIME updated_at
    }

    CLAIM_CODES {
        BIGINT claim_code_id PK
        BIGINT programme_id FK
        VARCHAR code_hash UK
        DATETIME expires_at
        DATETIME used_at
        BIGINT used_by_account_id FK
        DATETIME created_at
    }

    PROGRAMMES ||--o{ STUDENTS : "has"
    GROUPS ||--o{ STUDENTS : "contains"
    ACCOUNTS ||--o| STUDENTS : "belongs to"
    PROGRAMMES ||--o{ CLAIM_CODES : "issues"
    ACCOUNTS ||--o{ CLAIM_CODES : "uses"