CREATE TABLE knowledge_document (
    id VARCHAR(36) NOT NULL,
    source_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_document_source UNIQUE (source_key)
);

CREATE TABLE knowledge_document_version (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(300) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    publisher VARCHAR(200) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    reviewer VARCHAR(200) NOT NULL,
    retrieved_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6),
    retired_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_version UNIQUE (document_id, version_no),
    CONSTRAINT uk_knowledge_content UNIQUE (document_id, content_hash),
    CONSTRAINT fk_knowledge_version_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_knowledge_version_no CHECK (version_no >= 1),
    CONSTRAINT ck_knowledge_version_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'RETIRED')
    )
);

CREATE INDEX idx_knowledge_version_status
    ON knowledge_document_version (status, locale, published_at);

CREATE TABLE knowledge_chunk (
    id VARCHAR(36) NOT NULL,
    version_id VARCHAR(36) NOT NULL,
    ordinal INT NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_chunk_ordinal UNIQUE (version_id, ordinal),
    CONSTRAINT uk_knowledge_chunk_hash UNIQUE (version_id, content_hash),
    CONSTRAINT fk_knowledge_chunk_version
        FOREIGN KEY (version_id) REFERENCES knowledge_document_version (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_knowledge_chunk_ordinal CHECK (ordinal >= 1)
);
