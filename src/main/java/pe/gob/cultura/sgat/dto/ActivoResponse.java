package pe.gob.cultura.sgat.dto;

public record ActivoResponse(
        Long id,
        String codigoPatrimonial,
        String numeroSerie,
        String marca,
        String modelo,
        String categoria,
        String tipoActivo,
        String ubicacion,
        String estado,
        String condicionFisica) {
}
