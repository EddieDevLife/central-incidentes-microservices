package br.com.ada.incidentes.triagem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * A mensagem que chega pela fila, vista pelo lado de quem recebe.
 *
 * ignoreUnknown = true: se o abertura-service passar a mandar um campo novo,
 * este serviço simplesmente o ignora em vez de quebrar ("leitor tolerante").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncidenteAbertoEvent(
        UUID eventoId,
        UUID incidenteId,
        String titulo,
        Nivel impacto,
        Nivel urgencia,
        Instant abertoEm) {
}
