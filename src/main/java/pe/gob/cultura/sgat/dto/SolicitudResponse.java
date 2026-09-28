package pe.gob.cultura.sgat.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SolicitudResponse(
        Long id,
        String solicitante,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaAtencion,
        String motivo,
        String estado,
        String observacionesAtencion,
        List<DetalleResponse> detalles) {
}
