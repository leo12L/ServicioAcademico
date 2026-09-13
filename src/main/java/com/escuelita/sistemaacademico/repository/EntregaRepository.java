package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    Optional<Entrega> findByAlumnoIdAndActividadId(Long alumnoId, Long actividadId);
    List<Entrega> findByActividadId(Long actividadId);
    List<Entrega> findByAlumnoIdAndActividadCursoId(Long alumnoId, Long cursoId);
}
