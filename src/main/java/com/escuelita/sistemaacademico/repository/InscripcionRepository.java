package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByAlumnoId(Long alumnoId);
    List<Inscripcion> findByCursoId(Long cursoId);
}
