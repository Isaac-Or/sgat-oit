package pe.gob.cultura.sgat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByUsuarioSolicitanteIdOrderByFechaSolicitudDesc(Long usuarioId);

    List<Solicitud> findAllByOrderByFechaSolicitudDesc();

    List<Solicitud> findByEstadoOrderByFechaSolicitudDesc(String estado);

    @Query("select count(d) > 0 from SolicitudDetalle d "
            + "where d.solicitud.usuarioSolicitante.id = :usuarioId "
            + "and d.solicitud.estado = 'PENDIENTE' "
            + "and d.tipoActivo.id = :tipoId")
    boolean existePendienteConTipo(@Param("usuarioId") Long usuarioId, @Param("tipoId") Long tipoId);
}
