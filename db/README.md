# Esquema de base de datos

`001_schema_baseline.sql` representa el esquema base actualmente desplegado en Neon para SGAT-OIT.

El script crea las 14 tablas, restricciones, índices, triggers y datos iniciales del sistema. Está preparado exclusivamente para una base PostgreSQL vacía llamada `sgat_oit`; si detecta tablas del sistema, cancela la transacción sin borrar información.

Docker Compose monta este directorio en `/docker-entrypoint-initdb.d`, por lo que PostgreSQL ejecuta el script solamente al crear por primera vez el volumen local de datos.

No ejecute este archivo sobre la base existente de Neon. Los cambios posteriores del esquema deben agregarse como migraciones nuevas y versionadas.

Origen recibido: `base de datos.sql`

SHA-256 del archivo original: `9F8E75FDCE3799A8E09C8E43E2025D27C1C5B745E532665C299FB6BD70E619C0`