package br.com.ada.incidentes.triagem;

import java.time.Instant;
import java.util.UUID;

public record TriagemResponse(
        UUID incidenteId,
        String titulo,
        Prioridade prioridade,
        int prazoMinutos,
        Instant prazoLimite,
        Instant triadaEm) {

    public static TriagemResponse de(Triagem t) {
        return new TriagemResponse(
                t.getIncidenteId(), t.getTitulo(), t.getPrioridade(),
                t.getPrazoMinutos(), t.getPrazoLimite(), t.getTriadaEm());
    }
}
