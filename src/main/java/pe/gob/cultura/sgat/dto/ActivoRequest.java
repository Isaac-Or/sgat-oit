package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActivoRequest(
        @NotNull Long tipoActivoId,
        @NotNull Long ubicacionId,
        @NotBlank String codigoPatrimonial,
        String numeroSerie,
        @NotBlank String marca,
        @NotBlank String modelo,
        String descripcion,
        String estado,
        String condicionFisica) {
}
