package com.escuelita.sistemaacademico.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AvanceCursoResponse {

    private List<TemaAvanceDTO> temas;
    private int completados;
    private int total;
    private double porcentaje;

    public AvanceCursoResponse(List<TemaAvanceDTO> temas) {
        this.temas = temas;
        this.total = temas.size();
        this.completados = (int) temas.stream().filter(TemaAvanceDTO::isCompletado).count();
        this.porcentaje = total == 0 ? 0 : Math.round(completados * 1000.0 / total) / 10.0;
    }
}
