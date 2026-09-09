package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByMaestroId(Long maestroId);
}
