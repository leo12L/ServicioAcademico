package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.dto.AvanceCursoResponse;
import com.escuelita.sistemaacademico.model.Tema;
import com.escuelita.sistemaacademico.service.TemaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/temas")
@CrossOrigin(origins = "*")
public class TemaController {

    private final TemaService temaService;

    public TemaController(TemaService temaService) {
        this.temaService = temaService;
    }

    @GetMapping("/curso/{cursoId}")
    public List<Tema> listarPorCurso(@PathVariable Long cursoId) {
        return temaService.listarPorCurso(cursoId);
    }

    @PostMapping
    public Tema crear(@Valid @RequestBody Tema tema) {
        return temaService.crear(tema);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        temaService.eliminar(id);
    }

    @GetMapping("/avance")
    public AvanceCursoResponse avance(@RequestParam Long alumnoId, @RequestParam Long cursoId) {
        return temaService.avanceDeAlumnoEnCurso(alumnoId, cursoId);
    }

    @PatchMapping("/{temaId}/completar")
    public AvanceCursoResponse marcarCompletado(@PathVariable Long temaId, @RequestParam Long alumnoId) {
        return temaService.toggleCompletado(temaId, alumnoId);
    }
}
