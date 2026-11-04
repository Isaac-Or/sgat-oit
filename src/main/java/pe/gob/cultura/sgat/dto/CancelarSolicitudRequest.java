package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarSolicitudRequest(
        @NotBlank @Size(max = 1000) String motivo) {
}
