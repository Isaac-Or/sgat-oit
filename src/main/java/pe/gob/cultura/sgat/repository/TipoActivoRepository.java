package pe.gob.cultura.sgat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.TipoActivo;

public interface TipoActivoRepository extends JpaRepository<TipoActivo, Long> {
    List<TipoActivo> findByActivoLogicoTrueOrderByNombreAsc();
}
