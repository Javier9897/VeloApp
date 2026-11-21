CREATE TABLE IF NOT EXISTS usuarios (
    rut VARCHAR(12) PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    telefono VARCHAR(32) NOT NULL,
    clave VARCHAR(160) NOT NULL,
    rol VARCHAR(10) NOT NULL CHECK (rol IN ('PRODUCTOR', 'APLICADOR')),
    foto_perfil CLOB
);

CREATE TABLE IF NOT EXISTS sesiones (
    token_hash VARCHAR(64) PRIMARY KEY,
    usuario VARCHAR(12) NOT NULL REFERENCES usuarios(rut),
    expira BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS solicitudes (
    id VARCHAR(36) PRIMARY KEY,
    productor VARCHAR(12) NOT NULL REFERENCES usuarios(rut),
    producto VARCHAR(40) NOT NULL,
    hectareas DOUBLE PRECISION NOT NULL CHECK (hectareas > 0 AND hectareas <= 1000000),
    agua BOOLEAN NOT NULL,
    turno VARCHAR(20) NOT NULL,
    obstaculos VARCHAR(300) NOT NULL,
    comentario VARCHAR(1000) NOT NULL,
    fotos CLOB NOT NULL,
    creada BIGINT NOT NULL,
    aceptada VARCHAR(12) REFERENCES usuarios(rut)
);

CREATE TABLE IF NOT EXISTS ofertas (
    solicitud VARCHAR(36) NOT NULL REFERENCES solicitudes(id),
    aplicador VARCHAR(12) NOT NULL REFERENCES usuarios(rut),
    precio BIGINT NOT NULL CHECK (precio > 0 AND precio <= 9999999999),
    PRIMARY KEY (solicitud, aplicador)
);

CREATE INDEX IF NOT EXISTS idx_solicitudes_productor ON solicitudes(productor);
CREATE INDEX IF NOT EXISTS idx_sesiones_expira ON sesiones(expira);
