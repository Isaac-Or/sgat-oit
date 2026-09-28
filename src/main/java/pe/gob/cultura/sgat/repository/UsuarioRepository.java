package pe.gob.cultura.sgat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);
}
