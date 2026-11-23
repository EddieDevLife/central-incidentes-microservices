package br.com.ada.incidentes.triagem;

import jakarta.validation.constraints.Min;

public record AtualizarRegraRequest(@Min(1) int prazoMinutos) {
}
