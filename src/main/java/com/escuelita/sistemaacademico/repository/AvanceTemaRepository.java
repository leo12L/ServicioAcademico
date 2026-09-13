package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.AvanceTema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvanceTemaRepository extends JpaRepository<AvanceTema, Long> {
    List<AvanceTema> findByAlumnoIdAndTemaCursoId(Long alumnoId, Long cursoId);
    Optional<AvanceTema> findByAlumnoIdAndTemaId(Long alumnoId, Long temaId);
}
