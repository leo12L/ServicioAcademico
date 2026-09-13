package com.escuelita.sistemaacademico.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "entregas", uniqueConstraints = @UniqueConstraint(columnNames = {"alumno_id", "actividad_id"}))
@Getter
@Setter
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    private String nombreArchivoOriginal;

    private String rutaArchivo;

    private Double calificacion;

    private LocalDateTime fechaEntrega = LocalDateTime.now();
}
