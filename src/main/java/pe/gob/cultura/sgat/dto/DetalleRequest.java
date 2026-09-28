package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotNull;

public record DetalleRequest(@NotNull Long tipoActivoId, String observaciones) {
}
