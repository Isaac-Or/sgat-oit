package pe.gob.cultura.sgat.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    Page<Usuario> findByActivoLogicoTrue(Pageable pageable);

    Optional<Usuario> findByIdAndActivoLogicoTrue(Long id);

    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    @Query("select distinct u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdConRolesYPermisos(@Param("id") Long id);
}
