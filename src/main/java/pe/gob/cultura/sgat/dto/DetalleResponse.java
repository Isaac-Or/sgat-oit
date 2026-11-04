package pe.gob.cultura.sgat.dto;

public record DetalleResponse(
        Long id,
        Long tipoActivoId,
        String tipoActivo,
        String activo,
        String observaciones) {
}
