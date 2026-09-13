package com.escuelita.sistemaacademico.service;

import com.escuelita.sistemaacademico.dto.ActividadEstadoDTO;
import com.escuelita.sistemaacademico.model.Actividad;
import com.escuelita.sistemaacademico.model.EstadoInscripcion;
import com.escuelita.sistemaacademico.model.Entrega;
import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.repository.ActividadRepository;
import com.escuelita.sistemaacademico.repository.EntregaRepository;
import com.escuelita.sistemaacademico.repository.InscripcionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActividadService {

    private final ActividadRepository actividadRepository;
    private final EntregaRepository entregaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final Path directorioSubidas;

    public ActividadService(ActividadRepository actividadRepository,
                             EntregaRepository entregaRepository,
                             InscripcionRepository inscripcionRepository,
                             @Value("${app.upload-dir:uploads/entregas}") String directorioSubidas) {
        this.actividadRepository = actividadRepository;
        this.entregaRepository = entregaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.directorioSubidas = Path.of(directorioSubidas);
    }

    public Actividad crear(Actividad actividad) {
        return actividadRepository.save(actividad);
    }

    public List<Actividad> listarPorCurso(Long cursoId) {
        return actividadRepository.findByCursoId(cursoId);
    }

    public void eliminar(Long id) {
        actividadRepository.deleteById(id);
    }

    public List<Actividad> listarPendientes(Long alumnoId, Long cursoId) {
        List<Actividad> actividades = actividadRepository.findByCursoId(cursoId);
        Set<Long> entregadas = entregaRepository.findByAlumnoIdAndActividadCursoId(alumnoId, cursoId)
                .stream()
                .map(e -> e.getActividad().getId())
                .collect(Collectors.toSet());

        return actividades.stream()
                .filter(a -> !entregadas.contains(a.getId()))
                .collect(Collectors.toList());
    }

    public List<Actividad> listarPendientesAlumno(Long alumnoId) {
        List<Long> cursoIds = inscripcionRepository.findByAlumnoId(alumnoId).stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA)
                .map(i -> i.getCurso().getId())
                .distinct()
                .collect(Collectors.toList());

        return cursoIds.stream()
                .flatMap(cursoId -> listarPendientes(alumnoId, cursoId).stream())
                .collect(Collectors.toList());
    }

    public List<Entrega> listarEntregas(Long actividadId) {
        return entregaRepository.findByActividadId(actividadId);
    }

    public List<ActividadEstadoDTO> listarConEstadoParaAlumno(Long alumnoId, Long cursoId) {
        List<Actividad> actividades = actividadRepository.findByCursoId(cursoId);
        Map<Long, Entrega> entregasPorActividad = entregaRepository.findByAlumnoIdAndActividadCursoId(alumnoId, cursoId)
                .stream()
                .collect(Collectors.toMap(e -> e.getActividad().getId(), e -> e));

        return actividades.stream()
                .map(a -> {
                    Entrega entrega = entregasPorActividad.get(a.getId());
                    return new ActividadEstadoDTO(
                            a.getId(), a.getTipo(), a.getTitulo(), a.getDescripcion(),
                            entrega != null,
                            entrega != null ? entrega.getId() : null,
                            entrega != null ? entrega.getFechaEntrega() : null,
                            entrega != null ? entrega.getCalificacion() : null
                    );
                })
                .collect(Collectors.toList());
    }

    public Entrega entregar(Long actividadId, Long alumnoId, MultipartFile archivo) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        try {
            Files.createDirectories(directorioSubidas);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        String nombreGuardado = actividadId + "_" + alumnoId + "_" + System.currentTimeMillis() + "_" + nombreOriginal;
        Path destino = directorioSubidas.resolve(nombreGuardado);

        try {
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Entrega entrega = entregaRepository.findByAlumnoIdAndActividadId(alumnoId, actividadId)
                .orElseGet(Entrega::new);

        Usuario alumno = new Usuario();
        alumno.setId(alumnoId);

        entrega.setActividad(actividad);
        entrega.setAlumno(alumno);
        entrega.setNombreArchivoOriginal(nombreOriginal);
        entrega.setRutaArchivo(destino.toString());
        entrega.setFechaEntrega(java.time.LocalDateTime.now());

        return entregaRepository.save(entrega);
    }
}
