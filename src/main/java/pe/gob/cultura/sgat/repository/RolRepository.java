package pe.gob.cultura.sgat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByCodigo(String codigo);
}
