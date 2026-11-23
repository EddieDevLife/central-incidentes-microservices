package br.com.ada.incidentes.abertura;

import java.time.Instant;
import java.util.UUID;

/** O que a API devolve. Separado da entidade para a tabela poder mudar sem quebrar a API. */
public record IncidenteResponse(
        UUID id,
        String titulo,
        String descricao,
        Nivel impacto,
        Nivel urgencia,
        Instant abertoEm) {

    public static IncidenteResponse de(Incidente i) {
        return new IncidenteResponse(
                i.getId(), i.getTitulo(), i.getDescricao(),
                i.getImpacto(), i.getUrgencia(), i.getAbertoEm());
    }
}
