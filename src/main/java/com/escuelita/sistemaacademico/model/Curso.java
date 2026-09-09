package com.escuelita.sistemaacademico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cursos")
@Getter
@Setter
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoCurso tipo;

    @ManyToOne
    @JoinColumn(name = "maestro_id")
    private Usuario maestro;

    private boolean activo = true;
}
