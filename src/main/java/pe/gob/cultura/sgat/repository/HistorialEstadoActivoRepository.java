package pe.gob.cultura.sgat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.HistorialEstadoActivo;

public interface HistorialEstadoActivoRepository extends JpaRepository<HistorialEstadoActivo, Long> {
    List<HistorialEstadoActivo> findByActivoIdOrderByFechaCambioDesc(Long activoId);
}
