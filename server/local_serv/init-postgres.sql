-- init-zitadel.sql
-- Создаем необходимые схемы
CREATE SCHEMA IF NOT EXISTS system;
CREATE SCHEMA IF NOT EXISTS eventstore;
CREATE SCHEMA IF NOT EXISTS projections;

-- Таблица encryption_keys (ПРАВИЛЬНАЯ СТРУКТУРА)
CREATE TABLE IF NOT EXISTS system.encryption_keys (
                                                      id TEXT PRIMARY KEY,
                                                      name TEXT NOT NULL,
                                                      key BYTEA NOT NULL,           -- ← колонка называется "key"
                                                      created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Таблица событий
CREATE TABLE IF NOT EXISTS eventstore.events (
                                                 event_id TEXT NOT NULL,
                                                 aggregate_type TEXT NOT NULL,
                                                 aggregate_id TEXT NOT NULL,
                                                 aggregate_sequence INT NOT NULL,
                                                 aggregate_version TEXT,
                                                 event_type TEXT NOT NULL,
                                                 event_sequence BIGSERIAL NOT NULL,
                                                 event_data BYTEA,
                                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resource_owner TEXT,
    PRIMARY KEY (event_sequence),
    UNIQUE (aggregate_type, aggregate_id, aggregate_sequence)
    );

-- Индексы
CREATE INDEX IF NOT EXISTS idx_events_aggregate ON eventstore.events (aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON eventstore.events (created_at);

-- Таблица для миграций (если нужна)
CREATE TABLE IF NOT EXISTS system.migrations (
                                                 version TEXT PRIMARY KEY,
                                                 applied_at TIMESTAMP NOT NULL DEFAULT NOW()
    );