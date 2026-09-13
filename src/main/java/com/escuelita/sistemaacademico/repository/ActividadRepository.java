package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findByCursoId(Long cursoId);
}
