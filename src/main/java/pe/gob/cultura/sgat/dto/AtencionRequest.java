package pe.gob.cultura.sgat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AtencionRequest(
        @NotNull Boolean aprobada,
        String observaciones,
        List<@Valid ActivoElegido> activos) {
}
