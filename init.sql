-- ════════════════════════════════════════════════════════════════
--  Inicialización de la base de datos del sistema.
--
--  Este script lo ejecuta Postgres automáticamente la PRIMERA vez
--  que arranca el contenedor (gracias al mount en docker-compose).
--
--  PATRÓN: "database per service"
--   - checkin_db      → Microservicio Check-In   (este)
--   - seguridad_db    → Microservicio Seguridad  (compañero)
--   - aerolinea_db    → Microservicio Aerolínea  (compañero)
--   - distribucion_db → Microservicio Distribución (compañero)
--
--  Cada compañero agrega su CREATE DATABASE aquí y sus tablas
--  en otro archivo init-<servicio>.sql. TODOS los servicios
--  comparten la MISMA instancia de Postgres.
-- ════════════════════════════════════════════════════════════════

-- checkin_db ya se crea vía POSTGRES_DB, pero si otros equipos
-- necesitan DBs adicionales, las crean acá:
--
--   CREATE DATABASE seguridad_db;
--   CREATE DATABASE aerolinea_db;
--
-- (Descomenten cuando se integren.)

-- Generado completamente con ayuda de IA

-- ─── Tablas del microservicio Check-In ──────────────────────────
\connect checkin_db;

-- Pasajeros registrados
CREATE TABLE IF NOT EXISTS pasajeros (
    id         VARCHAR(64)  PRIMARY KEY,
    nombre     VARCHAR(255) NOT NULL,
    documento  VARCHAR(64)  NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL
);

-- Equipajes (maletas) registradas durante el check-in
CREATE TABLE IF NOT EXISTS equipajes (
    id                 VARCHAR(64)   PRIMARY KEY,
    codigo_rfid        VARCHAR(64)   NOT NULL UNIQUE,
    peso               NUMERIC(5, 2) NOT NULL CHECK (peso > 0),
    estado             VARCHAR(32)   NOT NULL,
    pasajero_id        VARCHAR(64)   NOT NULL REFERENCES pasajeros(id),
    vuelo_id           VARCHAR(32)   NOT NULL,
    timestamp_checkin  TIMESTAMPTZ   NOT NULL,
    timestamp_entrega  TIMESTAMPTZ,
    vehiculo_id        VARCHAR(64)
);

-- Índices para búsquedas típicas
CREATE INDEX IF NOT EXISTS idx_equipajes_pasajero ON equipajes(pasajero_id);
CREATE INDEX IF NOT EXISTS idx_equipajes_vuelo    ON equipajes(vuelo_id);
CREATE INDEX IF NOT EXISTS idx_equipajes_estado   ON equipajes(estado);

-- ─── Outbox Pattern ─────────────────────────────────────
-- Tabla donde Check-in escribe los eventos en la MISMA transacción
-- que los inserts de equipajes. Un relay aparte los lee y publica
-- a Kafka. Esto garantiza consistencia DB↔Kafka.
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id      UUID         PRIMARY KEY,
    topico        VARCHAR(64)  NOT NULL,
    clave         VARCHAR(64)  NOT NULL,
    payload       TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMPTZ,
    intentos      INT          NOT NULL DEFAULT 0,
    ultimo_error  TEXT
);

-- Índice parcial: solo los pendientes (published_at IS NULL).
-- Acelera el polling del relay.
CREATE INDEX IF NOT EXISTS idx_outbox_pendientes
    ON outbox_events(created_at)
    WHERE published_at IS NULL;