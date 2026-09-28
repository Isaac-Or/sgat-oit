package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotNull;

public record ActivoElegido(@NotNull Long detalleId, @NotNull Long activoId) {
}
