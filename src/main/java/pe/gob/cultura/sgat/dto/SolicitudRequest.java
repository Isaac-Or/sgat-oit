package pe.gob.cultura.sgat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SolicitudRequest(
        @NotBlank String motivo,
        @NotEmpty List<@Valid DetalleRequest> detalles) {
}
