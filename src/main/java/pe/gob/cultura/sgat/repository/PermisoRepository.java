package pe.gob.cultura.sgat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

}
