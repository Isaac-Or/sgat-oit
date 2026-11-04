
package pe.gob.cultura.sgat.dto;

import java.util.Set;

public record UsuarioResponse(
    Long id,
    String nombres,
    String apellidos,
    String correoInstitucional,
    boolean habilitado,
    Set<String> roles
) {}
