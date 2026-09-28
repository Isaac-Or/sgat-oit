package pe.gob.cultura.sgat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Activo;

public interface ActivoRepository extends JpaRepository<Activo, Long> {
    List<Activo> findByActivoLogicoTrueOrderByCodigoPatrimonialAsc();

    List<Activo> findByEstadoAndActivoLogicoTrueOrderByCodigoPatrimonialAsc(String estado);

    boolean existsByCodigoPatrimonial(String codigoPatrimonial);

    boolean existsByCodigoPatrimonialAndIdNot(String codigoPatrimonial, Long id);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, Long id);

    boolean existsByTipoActivoIdAndEstadoAndActivoLogicoTrue(Long tipoActivoId, String estado);
}
