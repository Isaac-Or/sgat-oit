package pe.gob.cultura.sgat.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.Asignacion;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    boolean existsByActivoIdAndFechaDevolucionRealIsNull(Long activoId);

    boolean existsByActivoId(Long activoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asignacion a where a.id = :id")
    Optional<Asignacion> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "solicitudDetalle", "solicitudDetalle.solicitud", "activo", "activo.tipoActivo",
        "usuarioReceptor", "entregadoPor", "recibidoDevolucionPor"
    })
    List<Asignacion> findAllByOrderByFechaEntregaDesc();

    @EntityGraph(attributePaths = {
        "solicitudDetalle", "solicitudDetalle.solicitud", "activo", "activo.tipoActivo",
        "usuarioReceptor", "entregadoPor", "recibidoDevolucionPor"
    })
    List<Asignacion> findByFechaDevolucionRealIsNullOrderByFechaEntregaDesc();
}
