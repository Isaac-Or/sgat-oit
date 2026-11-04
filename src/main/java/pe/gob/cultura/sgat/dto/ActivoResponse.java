package pe.gob.cultura.sgat.dto;

public record ActivoResponse(
        Long id,
        Long tipoActivoId,
        Long ubicacionId,
        String codigoPatrimonial,
        String numeroSerie,
        String marca,
        String modelo,
        String descripcion,
        String categoria,
        String tipoActivo,
        String ubicacion,
        String estado,
        String condicionFisica,
        boolean reservado) {
}
