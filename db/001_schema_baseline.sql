
BEGIN;
SET LOCAL search_path TO public, pg_catalog;

DO $comprobacion_previa$
BEGIN
    IF current_database() <> 'sgat_oit' THEN
        RAISE EXCEPTION
          'BASE INCORRECTA: conectado a %, se requiere sgat_oit', current_database();
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_tables
        WHERE schemaname = 'public'
          AND tablename IN (
            'roles','permisos','usuarios','usuario_rol','rol_permiso',
            'categorias','tipos_activo','ubicaciones','activos','solicitudes',
            'solicitud_detalles','asignaciones','mantenimientos','historial_estados_activo'
          )
    ) THEN
        RAISE EXCEPTION
          'Se encontraron tablas SGAT-OIT: use una base de datos vacia; no se borrara nada';
    END IF;
END;
$comprobacion_previa$;


CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE permisos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    correo_institucional VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por BIGINT,
    modificado_por BIGINT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (modificado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asignado_por BIGINT,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT,
    FOREIGN KEY (asignado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE rol_permiso (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT,
    FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE RESTRICT
);

CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tipos_activo (
    id BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (categoria_id, nombre),
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE RESTRICT
);

CREATE TABLE ubicaciones (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE activos (
    id BIGSERIAL PRIMARY KEY,
    tipo_activo_id BIGINT NOT NULL,
    ubicacion_id BIGINT NOT NULL,
    codigo_patrimonial VARCHAR(50) NOT NULL UNIQUE,
    numero_serie VARCHAR(100) UNIQUE,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    condicion_fisica VARCHAR(20) NOT NULL DEFAULT 'BUENO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por BIGINT NOT NULL,
    modificado_por BIGINT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, tipo_activo_id),
    FOREIGN KEY (tipo_activo_id) REFERENCES tipos_activo(id) ON DELETE RESTRICT,
    FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id) ON DELETE RESTRICT,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (modificado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE solicitudes (
    id BIGSERIAL PRIMARY KEY,
    usuario_solicitante_id BIGINT NOT NULL,
    usuario_atendedor_id BIGINT,
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_atencion TIMESTAMPTZ,
    motivo TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observaciones_atencion TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por BIGINT,
    modificado_por BIGINT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (usuario_solicitante_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_atendedor_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (modificado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE solicitud_detalles (
    id BIGSERIAL PRIMARY KEY,
    solicitud_id BIGINT NOT NULL,
    tipo_activo_id BIGINT NOT NULL,
    activo_id BIGINT,
    observaciones TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (solicitud_id, activo_id),
    UNIQUE (id, activo_id),
    FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE RESTRICT,
    FOREIGN KEY (tipo_activo_id) REFERENCES tipos_activo(id) ON DELETE RESTRICT,
    FOREIGN KEY (activo_id, tipo_activo_id) REFERENCES activos(id, tipo_activo_id) ON DELETE RESTRICT
);

CREATE TABLE asignaciones (
    id BIGSERIAL PRIMARY KEY,
    solicitud_detalle_id BIGINT NOT NULL UNIQUE,
    activo_id BIGINT NOT NULL,
    usuario_receptor_id BIGINT NOT NULL,
    entregado_por BIGINT NOT NULL,
    recibido_devolucion_por BIGINT,
    fecha_entrega TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_devolucion_estimada TIMESTAMPTZ NOT NULL,
    fecha_devolucion_real TIMESTAMPTZ,
    observaciones_entrega TEXT,
    observaciones_retorno TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por BIGINT,
    modificado_por BIGINT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (solicitud_detalle_id, activo_id) REFERENCES solicitud_detalles(id, activo_id) ON DELETE RESTRICT,
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_receptor_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (entregado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (recibido_devolucion_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (modificado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE mantenimientos (
    id BIGSERIAL PRIMARY KEY,
    activo_id BIGINT NOT NULL,
    tecnico_responsable_id BIGINT,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    fecha_inicio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMPTZ,
    diagnostico TEXT,
    acciones_realizadas TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por BIGINT,
    modificado_por BIGINT,
    activo_logico BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE RESTRICT,
    FOREIGN KEY (tecnico_responsable_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT,
    FOREIGN KEY (modificado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE historial_estados_activo (
    id BIGSERIAL PRIMARY KEY,
    activo_id BIGINT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    cambiado_por BIGINT NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT,
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE RESTRICT,
    FOREIGN KEY (cambiado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

-- 15. RESTRICCIONES DE DOMINIO Y FECHAS
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_correo
  CHECK (correo_institucional = lower(btrim(correo_institucional))
         AND position('@' IN correo_institucional) > 1);
ALTER TABLE activos ADD CONSTRAINT ck_activos_estado
  CHECK (estado IN ('DISPONIBLE','ASIGNADO','EN_MANTENIMIENTO','DE_BAJA'));
ALTER TABLE activos ADD CONSTRAINT ck_activos_condicion
  CHECK (condicion_fisica IN ('BUENO','REGULAR','MALO'));
ALTER TABLE activos ADD CONSTRAINT ck_activos_codigo
  CHECK (btrim(codigo_patrimonial) <> '');
ALTER TABLE activos ADD CONSTRAINT ck_activos_serie
  CHECK (numero_serie IS NULL OR btrim(numero_serie) <> '');
ALTER TABLE activos ADD CONSTRAINT ck_activos_version
  CHECK (version >= 0);
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_estado
  CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','CANCELADA'));
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_motivo
  CHECK (btrim(motivo) <> '');
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_atencion
  CHECK (estado NOT IN ('APROBADA','RECHAZADA')
         OR (usuario_atendedor_id IS NOT NULL AND fecha_atencion IS NOT NULL));
ALTER TABLE asignaciones ADD CONSTRAINT ck_asignaciones_estimada
  CHECK (fecha_devolucion_estimada >= fecha_entrega);
ALTER TABLE asignaciones ADD CONSTRAINT ck_asignaciones_real
  CHECK (fecha_devolucion_real IS NULL OR fecha_devolucion_real >= fecha_entrega);
ALTER TABLE asignaciones ADD CONSTRAINT ck_asignaciones_recepcion
  CHECK ((fecha_devolucion_real IS NULL AND recibido_devolucion_por IS NULL)
      OR (fecha_devolucion_real IS NOT NULL AND recibido_devolucion_por IS NOT NULL));
ALTER TABLE mantenimientos ADD CONSTRAINT ck_mantenimientos_tipo
  CHECK (tipo IN ('PREVENTIVO','CORRECTIVO'));
ALTER TABLE mantenimientos ADD CONSTRAINT ck_mantenimientos_estado
  CHECK (estado IN ('PROGRAMADO','EN_PROCESO','FINALIZADO','CANCELADO'));
ALTER TABLE mantenimientos ADD CONSTRAINT ck_mantenimientos_fechas
  CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio);
ALTER TABLE mantenimientos ADD CONSTRAINT ck_mantenimientos_finalizado
  CHECK (estado <> 'FINALIZADO' OR fecha_fin IS NOT NULL);
ALTER TABLE mantenimientos ADD CONSTRAINT ck_mantenimientos_en_proceso
  CHECK (estado <> 'EN_PROCESO' OR fecha_fin IS NULL);
ALTER TABLE historial_estados_activo ADD CONSTRAINT ck_historial_estado_anterior
  CHECK (estado_anterior IS NULL OR estado_anterior IN
    ('DISPONIBLE','ASIGNADO','EN_MANTENIMIENTO','DE_BAJA'));
ALTER TABLE historial_estados_activo ADD CONSTRAINT ck_historial_estado_nuevo
  CHECK (estado_nuevo IN ('DISPONIBLE','ASIGNADO','EN_MANTENIMIENTO','DE_BAJA'));

-- 16. INDICES. Los PK y UNIQUE ya cuentan con indices creados por PostgreSQL.
CREATE INDEX idx_usuario_rol_rol ON usuario_rol(rol_id);
CREATE INDEX idx_rol_permiso_permiso ON rol_permiso(permiso_id);
CREATE INDEX idx_tipos_activo_categoria ON tipos_activo(categoria_id);
CREATE INDEX idx_activos_estado_tipo ON activos(estado,tipo_activo_id)
  WHERE activo_logico = TRUE;
CREATE INDEX idx_activos_ubicacion ON activos(ubicacion_id);
CREATE INDEX idx_solicitudes_usuario_fecha ON solicitudes(usuario_solicitante_id,fecha_solicitud DESC);
CREATE INDEX idx_solicitudes_estado_fecha ON solicitudes(estado,fecha_solicitud DESC);
CREATE INDEX idx_solicitudes_atendedor ON solicitudes(usuario_atendedor_id);
CREATE INDEX idx_detalles_solicitud ON solicitud_detalles(solicitud_id);
CREATE INDEX idx_detalles_activo ON solicitud_detalles(activo_id);
CREATE INDEX idx_asignaciones_usuario ON asignaciones(usuario_receptor_id,fecha_entrega DESC);
CREATE INDEX idx_asignaciones_activo_fecha ON asignaciones(activo_id,fecha_entrega DESC);
CREATE INDEX idx_asignaciones_vencimiento ON asignaciones(fecha_devolucion_estimada)
  WHERE fecha_devolucion_real IS NULL;
CREATE INDEX idx_mantenimientos_activo ON mantenimientos(activo_id,fecha_inicio DESC);
CREATE INDEX idx_mantenimientos_tecnico ON mantenimientos(tecnico_responsable_id);
CREATE INDEX idx_historial_activo_fecha ON historial_estados_activo(activo_id,fecha_cambio DESC);
-- No permite dos prestamos abiertos sobre el mismo activo, incluso si hay soft delete.
CREATE UNIQUE INDEX ux_asignacion_activo_abierto ON asignaciones(activo_id)
  WHERE fecha_devolucion_real IS NULL;
CREATE UNIQUE INDEX ux_mantenimiento_en_proceso ON mantenimientos(activo_id)
  WHERE estado = 'EN_PROCESO' AND activo_logico = TRUE;

-- 17. TRIGGERS DE AUDITORIA
CREATE OR REPLACE FUNCTION fn_actualizar_fecha_modificacion()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.fecha_modificacion := now();
  RETURN NEW;
END;
$$;
CREATE TRIGGER tr_usuarios_fecha_modificacion BEFORE UPDATE ON usuarios
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();
CREATE TRIGGER tr_activos_fecha_modificacion BEFORE UPDATE ON activos
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();
CREATE TRIGGER tr_solicitudes_fecha_modificacion BEFORE UPDATE ON solicitudes
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();
CREATE TRIGGER tr_asignaciones_fecha_modificacion BEFORE UPDATE ON asignaciones
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();
CREATE TRIGGER tr_mantenimientos_fecha_modificacion BEFORE UPDATE ON mantenimientos
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();

-- La aplicacion debe asignar NEW.modificado_por en el UPDATE que cambia el estado.
CREATE OR REPLACE FUNCTION fn_registrar_historial_estado_activo()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO historial_estados_activo
      (activo_id, estado_anterior, estado_nuevo, cambiado_por, motivo)
    VALUES (NEW.id, NULL, NEW.estado, NEW.creado_por, 'Registro inicial del activo');
  ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
    IF NEW.modificado_por IS NULL THEN
      RAISE EXCEPTION 'Indicar modificado_por al cambiar el estado del activo';
    END IF;
    INSERT INTO historial_estados_activo
      (activo_id, estado_anterior, estado_nuevo, cambiado_por, motivo)
    VALUES (NEW.id, OLD.estado, NEW.estado, NEW.modificado_por, 'Cambio de estado del activo');
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER tr_historial_estados_activo
  AFTER INSERT OR UPDATE OF estado ON activos
  FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial_estado_activo();

-- 18. DATOS SEMILLA: NO se inserta un admin con clave insegura/falsa.
INSERT INTO roles(codigo,descripcion) VALUES
 ('ROLE_ADMIN','Administrador general de SGAT-OIT'),
 ('ROLE_TECNICO','Personal tecnico responsable de activos'),
 ('ROLE_SOLICITANTE','Colaborador que solicita equipos');
INSERT INTO permisos(codigo,descripcion) VALUES
 ('USUARIO_VER','Consultar usuarios'),
 ('USUARIO_GESTIONAR','Administrar usuarios'),
 ('ACTIVO_VER','Consultar inventario'),
 ('ACTIVO_CREAR','Registrar activos'),
 ('ACTIVO_EDITAR','Modificar activos'),
 ('ACTIVO_BAJA','Dar de baja activos'),
 ('SOLICITUD_CREAR','Registrar solicitudes'),
 ('SOLICITUD_VER_PROPIAS','Consultar solicitudes propias'),
 ('SOLICITUD_VER_TODAS','Consultar todas las solicitudes'),
 ('SOLICITUD_APROBAR','Aprobar o rechazar solicitudes'),
 ('ASIGNACION_REGISTRAR','Registrar entrega de equipos'),
 ('DEVOLUCION_REGISTRAR','Registrar devolucion de equipos'),
 ('MANTENIMIENTO_REGISTRAR','Registrar y actualizar mantenimientos'),
 ('REPORTE_VER','Consultar reportes generales');
INSERT INTO rol_permiso(rol_id,permiso_id)
  SELECT r.id,p.id FROM roles r CROSS JOIN permisos p
  WHERE r.codigo = 'ROLE_ADMIN';
INSERT INTO rol_permiso(rol_id,permiso_id)
  SELECT r.id,p.id FROM roles r CROSS JOIN permisos p
  WHERE r.codigo = 'ROLE_TECNICO' AND p.codigo IN
  ('ACTIVO_VER','ACTIVO_CREAR','ACTIVO_EDITAR','SOLICITUD_CREAR',
   'SOLICITUD_VER_PROPIAS','SOLICITUD_VER_TODAS','SOLICITUD_APROBAR',
   'ASIGNACION_REGISTRAR','DEVOLUCION_REGISTRAR','MANTENIMIENTO_REGISTRAR','REPORTE_VER');
INSERT INTO rol_permiso(rol_id,permiso_id)
  SELECT r.id,p.id FROM roles r CROSS JOIN permisos p
  WHERE r.codigo = 'ROLE_SOLICITANTE' AND p.codigo IN
  ('ACTIVO_VER','SOLICITUD_CREAR','SOLICITUD_VER_PROPIAS');
INSERT INTO categorias(nombre,descripcion) VALUES
 ('Equipos de computo','Computadoras y equipos de procesamiento'),
 ('Perifericos','Dispositivos de entrada y salida'),
 ('Equipos de red','Dispositivos de conectividad y comunicaciones'),
 ('Equipos de impresion','Impresoras y equipos multifuncionales'),
 ('Equipos de almacenamiento','Dispositivos para almacenar datos'),
 ('Equipos de seguridad','Dispositivos fisicos de seguridad informatica');
INSERT INTO tipos_activo(categoria_id,nombre,descripcion)
SELECT c.id,t.nombre,t.descripcion
FROM (VALUES
 ('Equipos de computo','Laptop','Computadora portatil'),
 ('Equipos de computo','Computadora de escritorio','Computadora personal fija'),
 ('Equipos de computo','Servidor','Servidor fisico'),
 ('Perifericos','Monitor','Pantalla de visualizacion'),
 ('Perifericos','Teclado','Dispositivo de entrada'),
 ('Perifericos','Mouse','Dispositivo apuntador'),
 ('Perifericos','Lectora','Dispositivo de lectura'),
 ('Equipos de red','Switch','Conmutador de red'),
 ('Equipos de red','Router','Enrutador de red'),
 ('Equipos de red','Access Point','Punto de acceso inalambrico'),
 ('Equipos de impresion','Impresora','Equipo de impresion'),
 ('Equipos de almacenamiento','NAS','Almacenamiento conectado a red'),
 ('Equipos de seguridad','Firewall','Equipo de seguridad perimetral')
) AS t(categoria,nombre,descripcion)
JOIN categorias c ON c.nombre = t.categoria;
INSERT INTO ubicaciones(codigo,nombre,descripcion) VALUES
 ('OIT','Oficina de Infraestructura Tecnologica','Area de infraestructura'),
 ('ALMACEN','Almacen de equipos','Custodia de activos disponibles'),
 ('SOPORTE','Area de soporte tecnico','Revision y mantenimiento'),
 ('DATACENTER','Centro de Datos','Infraestructura de procesamiento');

-- 19. ADMINISTRADOR DE ARRANQUE: hash BCrypt aleatorio, no hay clave compartida.
-- La cuenta permanece deshabilitada hasta que una persona autorizada establezca
-- una nueva contrasena BCrypt con Spring Security y la habilite expresamente.
INSERT INTO usuarios
  (nombres, apellidos, correo_institucional, password_hash, habilitado, activo_logico)
VALUES
  ('Administrador', 'SGAT-OIT', 'admin@cultura.gob.pe',
   '$2y$12$rbui/6O8hWLUGDYHhkeBT.zPPQB0eGIzY8m59zvIfq8VUR8J0ydAq', FALSE, TRUE);

INSERT INTO usuario_rol (usuario_id, rol_id, asignado_por)
SELECT u.id, r.id, u.id
FROM usuarios u
JOIN roles r ON r.codigo = 'ROLE_ADMIN'
WHERE u.correo_institucional = 'admin@cultura.gob.pe';

-- 20. AUTOCOMPROBACION. Cualquier fallo cancela TODA la transaccion.
DO $validacion_final$
DECLARE
    n_tablas integer;
    n_columnas integer;
    n_fks integer;
    n_indices_sgat integer;
    n_triggers integer;
    n_roles integer;
    n_permisos integer;
    n_categorias integer;
    n_tipos integer;
    n_ubicaciones integer;
    n_rol_permiso integer;
    n_admin integer;
BEGIN
    SELECT count(*) INTO n_tablas
    FROM information_schema.tables
    WHERE table_schema='public' AND table_type='BASE TABLE'
      AND table_name IN (
          'roles','permisos','usuarios','usuario_rol','rol_permiso',
          'categorias','tipos_activo','ubicaciones','activos','solicitudes',
          'solicitud_detalles','asignaciones','mantenimientos','historial_estados_activo'
      );
    IF n_tablas <> 14 THEN
        RAISE EXCEPTION 'Tablas SGAT-OIT: % (esperado: 14)', n_tablas;
    END IF;

    SELECT count(*) INTO n_columnas
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name IN (
          'roles','permisos','usuarios','usuario_rol','rol_permiso',
          'categorias','tipos_activo','ubicaciones','activos','solicitudes',
          'solicitud_detalles','asignaciones','mantenimientos','historial_estados_activo'
      );
    IF n_columnas <> 110 THEN
        RAISE EXCEPTION 'Columnas SGAT-OIT: % (esperado: 110)', n_columnas;
    END IF;

    SELECT count(*) INTO n_fks
    FROM pg_catalog.pg_constraint c
    JOIN pg_catalog.pg_namespace ns ON ns.oid = c.connamespace
    WHERE ns.nspname = 'public' AND c.contype = 'f'
      AND c.conrelid IN (
          SELECT cls.oid FROM pg_catalog.pg_class cls
          JOIN pg_catalog.pg_namespace n ON n.oid = cls.relnamespace
          WHERE n.nspname = 'public' AND cls.relname IN (
             'roles','permisos','usuarios','usuario_rol','rol_permiso',
             'categorias','tipos_activo','ubicaciones','activos','solicitudes',
             'solicitud_detalles','asignaciones','mantenimientos','historial_estados_activo'
          )
      );
    IF n_fks <> 32 THEN
        RAISE EXCEPTION 'Claves foraneas SGAT-OIT: % (esperado: 32)', n_fks;
    END IF;

    SELECT count(*) INTO n_triggers
    FROM pg_catalog.pg_trigger t
    JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid
    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname='public' AND NOT t.tgisinternal
      AND t.tgname IN (
        'tr_usuarios_fecha_modificacion',
        'tr_activos_fecha_modificacion',
        'tr_solicitudes_fecha_modificacion',
        'tr_asignaciones_fecha_modificacion',
        'tr_mantenimientos_fecha_modificacion',
        'tr_historial_estados_activo'
      );
    IF n_triggers <> 6 THEN
        RAISE EXCEPTION 'Triggers SGAT-OIT: % (esperado: 6)', n_triggers;
    END IF;

    SELECT count(*) INTO n_indices_sgat
    FROM pg_catalog.pg_indexes
    WHERE schemaname = 'public'
      AND indexname IN (
       'idx_usuario_rol_rol','idx_rol_permiso_permiso',
       'idx_tipos_activo_categoria','idx_activos_estado_tipo',
       'idx_activos_ubicacion','idx_solicitudes_usuario_fecha',
       'idx_solicitudes_estado_fecha','idx_solicitudes_atendedor',
       'idx_detalles_solicitud','idx_detalles_activo',
       'idx_asignaciones_usuario','idx_asignaciones_activo_fecha',
       'idx_asignaciones_vencimiento','idx_mantenimientos_activo',
       'idx_mantenimientos_tecnico','idx_historial_activo_fecha',
       'ux_asignacion_activo_abierto','ux_mantenimiento_en_proceso'
      );
    IF n_indices_sgat <> 18 THEN
        RAISE EXCEPTION 'Indices SGAT-OIT: % (esperado: 18)', n_indices_sgat;
    END IF;

    SELECT count(*) INTO n_roles FROM roles;
    SELECT count(*) INTO n_permisos FROM permisos;
    SELECT count(*) INTO n_categorias FROM categorias;
    SELECT count(*) INTO n_tipos FROM tipos_activo;
    SELECT count(*) INTO n_ubicaciones FROM ubicaciones;
    SELECT count(*) INTO n_rol_permiso FROM rol_permiso;
    SELECT count(*) INTO n_admin
    FROM usuarios u JOIN usuario_rol ur ON ur.usuario_id=u.id
    JOIN roles r ON r.id=ur.rol_id
    WHERE u.correo_institucional='admin@cultura.gob.pe'
      AND r.codigo='ROLE_ADMIN' AND u.habilitado=FALSE;
    IF (n_roles,n_permisos,n_categorias,n_tipos,n_ubicaciones,n_rol_permiso,n_admin)
        <> (3,14,6,13,4,28,1) THEN
        RAISE EXCEPTION
          'Semillas incorrectas: roles=%, permisos=%, categorias=%, tipos=%, ubicaciones=%, rol_permiso=%, admin_inactivo=%',
          n_roles,n_permisos,n_categorias,n_tipos,n_ubicaciones,n_rol_permiso,n_admin;
    END IF;
END;
$validacion_final$;

COMMIT;

-- RESUMEN DE ENTREGA: debe mostrar 14 tablas y 32 relaciones FK.
SELECT
    current_database() AS base_datos,
    (SELECT count(*) FROM information_schema.tables
      WHERE table_schema='public' AND table_type='BASE TABLE') AS tablas,
    (SELECT count(*) FROM information_schema.columns
      WHERE table_schema='public') AS columnas,
    (SELECT count(*) FROM pg_catalog.pg_constraint c
      JOIN pg_catalog.pg_namespace n ON n.oid=c.connamespace
      WHERE n.nspname='public' AND c.contype='f') AS claves_foraneas,
    (SELECT count(*) FROM roles) AS roles,
    (SELECT count(*) FROM permisos) AS permisos,
    (SELECT count(*) FROM categorias) AS categorias,
    (SELECT count(*) FROM tipos_activo) AS tipos_activo,
    (SELECT count(*) FROM ubicaciones) AS ubicaciones,
    (SELECT count(*) FROM usuarios WHERE habilitado=FALSE
       AND correo_institucional='admin@cultura.gob.pe') AS admin_pendiente_activacion;
