package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.dto.ActividadEstadoDTO;
import com.escuelita.sistemaacademico.model.Actividad;
import com.escuelita.sistemaacademico.model.Entrega;
import com.escuelita.sistemaacademico.service.ActividadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/actividades")
@CrossOrigin(origins = "*")
public class ActividadController {

    private final ActividadService actividadService;

    public ActividadController(ActividadService actividadService) {
        this.actividadService = actividadService;
    }

    @PostMapping
    public Actividad crear(@Valid @RequestBody Actividad actividad) {
        return actividadService.crear(actividad);
    }

    @GetMapping("/curso/{cursoId}")
    public List<Actividad> listarPorCurso(@PathVariable Long cursoId) {
        return actividadService.listarPorCurso(cursoId);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        actividadService.eliminar(id);
    }

    @GetMapping("/pendientes")
    public List<Actividad> pendientes(@RequestParam Long alumnoId, @RequestParam Long cursoId) {
        return actividadService.listarPendientes(alumnoId, cursoId);
    }

    @GetMapping("/pendientes/alumno/{alumnoId}")
    public List<Actividad> pendientesDeTodosLosCursos(@PathVariable Long alumnoId) {
        return actividadService.listarPendientesAlumno(alumnoId);
    }

    @GetMapping("/estado")
    public List<ActividadEstadoDTO> estadoParaAlumno(@RequestParam Long alumnoId, @RequestParam Long cursoId) {
        return actividadService.listarConEstadoParaAlumno(alumnoId, cursoId);
    }

    @GetMapping("/{id}/entregas")
    public List<Entrega> entregas(@PathVariable Long id) {
        return actividadService.listarEntregas(id);
    }

    @PostMapping("/{id}/entregar")
    public Entrega entregar(@PathVariable Long id,
                             @RequestParam Long alumnoId,
                             @RequestParam MultipartFile archivo) {
        return actividadService.entregar(id, alumnoId, archivo);
    }
}
