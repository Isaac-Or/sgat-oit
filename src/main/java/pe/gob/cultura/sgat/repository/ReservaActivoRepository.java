package pe.gob.cultura.sgat.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.ReservaActivo;

public interface ReservaActivoRepository extends JpaRepository<ReservaActivo, Long> {

    boolean existsByActivoIdAndEstado(Long activoId, String estado);

    @Query("select r.activo.id from ReservaActivo r where r.estado = 'ACTIVA'")
    List<Long> findIdsActivosConReservaActiva();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaActivo r "
            + "join fetch r.activo "
            + "where r.solicitudDetalle.solicitud.id = :solicitudId "
            + "and r.estado = 'ACTIVA' order by r.activo.id")
    List<ReservaActivo> findActivasBySolicitudIdForUpdate(@Param("solicitudId") Long solicitudId);

    long countByActivoIdAndEstado(Long activoId, String estado);

    long countBySolicitudDetalleSolicitudIdAndEstado(Long solicitudId, String estado);

    @Query("select count(r) from ReservaActivo r where r.activo.id in :activoIds and r.estado = 'ACTIVA'")
    long countActivasByActivoIds(@Param("activoIds") Collection<Long> activoIds);
}
