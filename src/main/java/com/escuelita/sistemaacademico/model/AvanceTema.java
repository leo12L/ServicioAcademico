package com.escuelita.sistemaacademico.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "avances_tema", uniqueConstraints = @UniqueConstraint(columnNames = {"alumno_id", "tema_id"}))
@Getter
@Setter
public class AvanceTema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    @ManyToOne
    @JoinColumn(name = "tema_id")
    private Tema tema;

    private LocalDateTime fechaCompletado = LocalDateTime.now();
}
