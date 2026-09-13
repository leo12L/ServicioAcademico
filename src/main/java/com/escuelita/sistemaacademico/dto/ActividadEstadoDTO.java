package com.escuelita.sistemaacademico.dto;

import com.escuelita.sistemaacademico.model.TipoActividad;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ActividadEstadoDTO {

    private Long id;
    private TipoActividad tipo;
    private String titulo;
    private String descripcion;
    private boolean entregado;
    private Long entregaId;
    private LocalDateTime fechaEntrega;
    private Double calificacion;

    public ActividadEstadoDTO(Long id, TipoActividad tipo, String titulo, String descripcion,
                               boolean entregado, Long entregaId, LocalDateTime fechaEntrega, Double calificacion) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.entregado = entregado;
        this.entregaId = entregaId;
        this.fechaEntrega = fechaEntrega;
        this.calificacion = calificacion;
    }
}
