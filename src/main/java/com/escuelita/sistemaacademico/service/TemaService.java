package com.escuelita.sistemaacademico.service;

import com.escuelita.sistemaacademico.dto.AvanceCursoResponse;
import com.escuelita.sistemaacademico.dto.TemaAvanceDTO;
import com.escuelita.sistemaacademico.model.AvanceTema;
import com.escuelita.sistemaacademico.model.Tema;
import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.repository.AvanceTemaRepository;
import com.escuelita.sistemaacademico.repository.TemaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TemaService {

    private final TemaRepository temaRepository;
    private final AvanceTemaRepository avanceTemaRepository;

    public TemaService(TemaRepository temaRepository, AvanceTemaRepository avanceTemaRepository) {
        this.temaRepository = temaRepository;
        this.avanceTemaRepository = avanceTemaRepository;
    }

    public List<Tema> listarPorCurso(Long cursoId) {
        return temaRepository.findByCursoIdOrderByOrdenAsc(cursoId);
    }

    public Tema crear(Tema tema) {
        return temaRepository.save(tema);
    }

    public void eliminar(Long id) {
        temaRepository.deleteById(id);
    }

    public AvanceCursoResponse avanceDeAlumnoEnCurso(Long alumnoId, Long cursoId) {
        List<Tema> temas = temaRepository.findByCursoIdOrderByOrdenAsc(cursoId);
        Set<Long> temasCompletados = avanceTemaRepository.findByAlumnoIdAndTemaCursoId(alumnoId, cursoId)
                .stream()
                .map(a -> a.getTema().getId())
                .collect(Collectors.toSet());

        List<TemaAvanceDTO> temasConAvance = temas.stream()
                .map(t -> new TemaAvanceDTO(t.getId(), t.getTitulo(), t.getDescripcion(), t.getOrden(),
                        temasCompletados.contains(t.getId())))
                .collect(Collectors.toList());

        return new AvanceCursoResponse(temasConAvance);
    }

    public AvanceCursoResponse toggleCompletado(Long temaId, Long alumnoId) {
        Tema tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new IllegalArgumentException("Tema no encontrado: " + temaId));

        avanceTemaRepository.findByAlumnoIdAndTemaId(alumnoId, temaId)
                .ifPresentOrElse(
                        avanceTemaRepository::delete,
                        () -> {
                            AvanceTema nuevo = new AvanceTema();
                            Usuario alumno = new Usuario();
                            alumno.setId(alumnoId);
                            nuevo.setAlumno(alumno);
                            nuevo.setTema(tema);
                            avanceTemaRepository.save(nuevo);
                        }
                );

        return avanceDeAlumnoEnCurso(alumnoId, tema.getCurso().getId());
    }
}
