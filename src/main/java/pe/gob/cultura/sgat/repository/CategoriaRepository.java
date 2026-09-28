package pe.gob.cultura.sgat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.cultura.sgat.model.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

}
