package pe.gob.cultura.sgat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Ubicacion;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
    List<Ubicacion> findByActivoLogicoTrueOrderByNombreAsc();
}
