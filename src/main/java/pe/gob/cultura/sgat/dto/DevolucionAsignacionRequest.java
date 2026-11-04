package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DevolucionAsignacionRequest(
        @NotBlank
        @Pattern(regexp = "BUENO|REGULAR|MALO")
        String condicionFisica,
        @NotBlank
        @Pattern(regexp = "DISPONIBLE|EN_MANTENIMIENTO")
        String estadoActivo,
        @Size(max = 2000)
        String observaciones) {
}
