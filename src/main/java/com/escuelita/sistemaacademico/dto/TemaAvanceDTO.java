package com.escuelita.sistemaacademico.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemaAvanceDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private int orden;
    private boolean completado;

    public TemaAvanceDTO(Long id, String titulo, String descripcion, int orden, boolean completado) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.orden = orden;
        this.completado = completado;
    }
}
