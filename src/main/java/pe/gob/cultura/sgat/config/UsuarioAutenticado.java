package pe.gob.cultura.sgat.config;

/** Datos del usuario que hizo la peticion, extraidos del token JWT. */
public record UsuarioAutenticado(Long id, String correo) {
}
