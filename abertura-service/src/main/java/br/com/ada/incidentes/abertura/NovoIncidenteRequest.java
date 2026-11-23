package br.com.ada.incidentes.abertura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** O que o cliente manda no POST /incidentes. */
public record NovoIncidenteRequest(
        @NotBlank String titulo,
        String descricao,
        @NotNull Nivel impacto,
        @NotNull Nivel urgencia) {
}
