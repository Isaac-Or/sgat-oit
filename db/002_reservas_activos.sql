BEGIN;
SET LOCAL search_path TO public, pg_catalog;

DO $comprobacion_previa$
BEGIN
    IF current_database() <> 'sgat_oit' THEN
        RAISE EXCEPTION
          'BASE INCORRECTA: conectado a %, se requiere sgat_oit', current_database();
    END IF;

    IF to_regclass('public.reservas_activos') IS NOT NULL THEN
        RAISE EXCEPTION
          'La tabla reservas_activos ya existe; la migracion 002 no se aplicara dos veces';
    END IF;

    IF to_regclass('public.activos') IS NULL
       OR to_regclass('public.solicitud_detalles') IS NULL
       OR to_regclass('public.usuarios') IS NULL THEN
        RAISE EXCEPTION
          'La migracion 001 debe ejecutarse antes de la migracion 002';
    END IF;
END;
$comprobacion_previa$;

CREATE TABLE reservas_activos (
    id BIGSERIAL PRIMARY KEY,
    activo_id BIGINT NOT NULL,
    solicitud_detalle_id BIGINT NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_responsable_id BIGINT NOT NULL,
    fecha_finalizacion TIMESTAMPTZ,
    usuario_finalizador_id BIGINT,
    motivo_finalizacion TEXT,
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE RESTRICT,
    FOREIGN KEY (solicitud_detalle_id) REFERENCES solicitud_detalles(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_finalizador_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    CHECK (estado IN ('ACTIVA', 'LIBERADA', 'CONSUMIDA')),
    CHECK (
        (estado = 'ACTIVA'
         AND fecha_finalizacion IS NULL
         AND usuario_finalizador_id IS NULL
         AND motivo_finalizacion IS NULL)
        OR
        (estado IN ('LIBERADA', 'CONSUMIDA')
         AND fecha_finalizacion IS NOT NULL
         AND usuario_finalizador_id IS NOT NULL
         AND motivo_finalizacion IS NOT NULL
         AND btrim(motivo_finalizacion) <> '')
    )
);

CREATE UNIQUE INDEX ux_reserva_activo_activa
    ON reservas_activos(activo_id)
    WHERE estado = 'ACTIVA';

CREATE INDEX idx_reservas_detalle
    ON reservas_activos(solicitud_detalle_id);

CREATE INDEX idx_reservas_estado_fecha
    ON reservas_activos(estado, fecha_creacion DESC);

COMMENT ON TABLE reservas_activos IS
  'Reserva logica creada al aprobar una solicitud; no representa entrega fisica.';
COMMENT ON COLUMN reservas_activos.estado IS
  'ACTIVA durante la reserva, LIBERADA por cancelacion, CONSUMIDA por una futura entrega fisica.';
COMMENT ON COLUMN reservas_activos.motivo_finalizacion IS
  'La entrega futura debe consumir la reserva y crear la asignacion en la misma transaccion.';

DO $validacion_final$
BEGIN
    IF to_regclass('public.reservas_activos') IS NULL THEN
        RAISE EXCEPTION 'No se creo la tabla reservas_activos';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_indexes
        WHERE schemaname = 'public'
          AND indexname = 'ux_reserva_activo_activa'
    ) THEN
        RAISE EXCEPTION 'No se creo la unicidad de reserva activa por activo';
    END IF;
END;
$validacion_final$;

COMMIT;
