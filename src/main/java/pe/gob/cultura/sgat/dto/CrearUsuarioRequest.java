
package pe.gob.cultura.sgat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CrearUsuarioRequest(

    @NotBlank
    @Size(max = 100)
    String nombres,

    @NotBlank
    @Size(max = 150)
    String apellidos,

    @NotBlank
    @Email
    @Size(max = 254)
    String correoInstitucional,

    @NotBlank
    @Size(min = 12, max = 72)
    String password,

    @NotEmpty
    Set<
        @NotNull
        @Pattern(regexp = "ROLE_(ADMIN|TECNICO|SOLICITANTE)")
        String
    > roles

) {
    @Override
    public String toString() {
        return "CrearUsuarioRequest[nombres=" + nombres
                + ", apellidos=" + apellidos
                + ", correoInstitucional=" + correoInstitucional
                + ", password=[PROTECTED], roles=" + roles + "]";
    }
}
