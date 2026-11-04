package pe.gob.cultura.sgat.dto;

import java.time.OffsetDateTime;

public record AsignacionResponse(
        Long id,
        Long solicitudId,
        Long solicitudDetalleId,
        Long activoId,
        String codigoPatrimonial,
        String tipoActivo,
        Long usuarioReceptorId,
        String usuarioReceptor,
        String entregadoPor,
        String recibidoDevolucionPor,
        OffsetDateTime fechaEntrega,
        OffsetDateTime fechaDevolucionEstimada,
        OffsetDateTime fechaDevolucionReal,
        String observacionesEntrega,
        String observacionesRetorno,
        String estadoActivo,
        String condicionFisica,
        boolean abierta) {
}
