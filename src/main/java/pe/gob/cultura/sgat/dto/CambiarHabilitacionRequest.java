package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarHabilitacionRequest(
        @NotNull Boolean habilitado
) {}
