package pe.gob.cultura.sgat.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.Activo;

public interface ActivoRepository extends JpaRepository<Activo, Long> {
    List<Activo> findByActivoLogicoTrueOrderByCodigoPatrimonialAsc();

    List<Activo> findByEstadoAndActivoLogicoTrueOrderByCodigoPatrimonialAsc(String estado);

    boolean existsByCodigoPatrimonial(String codigoPatrimonial);

    boolean existsByCodigoPatrimonialAndIdNot(String codigoPatrimonial, Long id);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, Long id);

    boolean existsByTipoActivoIdAndEstadoAndActivoLogicoTrue(Long tipoActivoId, String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activo a where a.id in :ids order by a.id")
    List<Activo> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activo a where a.id = :id")
    java.util.Optional<Activo> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(a) > 0 from Activo a "
            + "where a.tipoActivo.id = :tipoId and a.estado = 'DISPONIBLE' and a.activoLogico = true "
            + "and not exists (select r.id from ReservaActivo r "
            + "where r.activo = a and r.estado = 'ACTIVA') "
            + "and not exists (select asi.id from Asignacion asi "
            + "where asi.activo = a and asi.fechaDevolucionReal is null)")
    boolean existsDisponibleSinReservaNiAsignacion(@Param("tipoId") Long tipoId);
}
