package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.model.Inscripcion;
import com.escuelita.sistemaacademico.service.InscripcionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "*")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @GetMapping
    public List<Inscripcion> listar() {
        return inscripcionService.listarTodas();
    }

    @GetMapping("/alumno/{alumnoId}")
    public List<Inscripcion> porAlumno(@PathVariable Long alumnoId) {
        return inscripcionService.listarPorAlumno(alumnoId);
    }

    @GetMapping("/curso/{cursoId}")
    public List<Inscripcion> porCurso(@PathVariable Long cursoId) {
        return inscripcionService.listarPorCurso(cursoId);
    }

    @PostMapping
    public Inscripcion crear(@Valid @RequestBody Inscripcion inscripcion) {
        return inscripcionService.guardar(inscripcion);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        inscripcionService.eliminar(id);
    }
}
