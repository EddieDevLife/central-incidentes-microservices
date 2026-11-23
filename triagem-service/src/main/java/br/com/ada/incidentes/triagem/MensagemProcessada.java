package br.com.ada.incidentes.triagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O "caderninho" de mensagens já atendidas.
 *
 * O eventoId é a CHAVE PRIMÁRIA: o próprio banco se recusa a anotar o mesmo
 * eventoId duas vezes. É a garantia final de que a mensagem repetida não
 * duplica nada, mesmo que duas cópias cheguem ao mesmo tempo.
 */
@Entity
@Table(name = "mensagens_processadas")
public class MensagemProcessada {

    @Id
    private UUID eventoId;

    @Column(nullable = false)
    private Instant processadaEm;

    protected MensagemProcessada() {
    }

    public MensagemProcessada(UUID eventoId) {
        this.eventoId = eventoId;
        this.processadaEm = Instant.now();
    }

    public UUID getEventoId() { return eventoId; }
    public Instant getProcessadaEm() { return processadaEm; }
}
