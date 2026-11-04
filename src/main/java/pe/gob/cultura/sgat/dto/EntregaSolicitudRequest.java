package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record EntregaSolicitudRequest(
        @NotNull @Future OffsetDateTime fechaDevolucionEstimada,
        @Size(max = 2000) String observaciones) {
}
