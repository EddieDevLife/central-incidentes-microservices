package br.com.ada.incidentes.abertura;

import java.time.Instant;
import java.util.UUID;

/**
 * A mensagem que vai para a fila. É o "contrato" entre os dois serviços.
 *
 * O triagem-service tem a SUA PRÓPRIA cópia desta classe: os serviços não
 * compartilham código, só o formato da mensagem (JSON).
 */
public record IncidenteAbertoEvent(
        UUID eventoId,
        UUID incidenteId,
        String titulo,
        Nivel impacto,
        Nivel urgencia,
        Instant abertoEm) {
}
