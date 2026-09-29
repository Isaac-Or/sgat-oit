package pe.gob.cultura.sgat.dto;

import java.time.OffsetDateTime;

public record HistorialResponse(
        Long id,
        String estadoAnterior,
        String estadoNuevo,
        OffsetDateTime fechaCambio,
        String motivo,
        String usuarioResponsable) {
}