package pe.gob.cultura.sgat.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.cultura.sgat.model.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    Page<Usuario> findByActivoLogicoTrue(Pageable pageable);

    Optional<Usuario> findByIdAndActivoLogicoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.activoLogico = true")
    Optional<Usuario> findActivoByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(distinct u.id)
            from Usuario u join u.roles r
            where u.habilitado = true
              and u.activoLogico = true
              and r.activoLogico = true
              and r.codigo = 'ROLE_ADMIN'
            """)
    long countAdministradoresActivos();

    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    @Query("select distinct u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdConRolesYPermisos(@Param("id") Long id);
}
