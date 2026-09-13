package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Tema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemaRepository extends JpaRepository<Tema, Long> {
    List<Tema> findByCursoIdOrderByOrdenAsc(Long cursoId);
}
