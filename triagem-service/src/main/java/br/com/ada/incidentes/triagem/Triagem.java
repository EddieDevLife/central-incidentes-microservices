package br.com.ada.incidentes.triagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** O resultado da triagem de um incidente: a prioridade e até quando ele precisa ser resolvido. */
@Entity
@Table(name = "triagens")
public class Triagem {

    @Id
    private UUID id;

    // Este serviço guarda só o ID do incidente. Os dados completos moram no
    // banco do abertura-service, e este serviço nunca lê aquele banco.
    @Column(nullable = false, unique = true)
    private UUID incidenteId;

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade;

    @Column(nullable = false)
    private int prazoMinutos;

    @Column(nullable = false)
    private Instant prazoLimite;

    @Column(nullable = false)
    private Instant triadaEm;

    protected Triagem() {
    }

    public Triagem(UUID incidenteId, String titulo, Prioridade prioridade,
                   int prazoMinutos, Instant prazoLimite) {
        this.id = UUID.randomUUID();
        this.incidenteId = incidenteId;
        this.titulo = titulo;
        this.prioridade = prioridade;
        this.prazoMinutos = prazoMinutos;
        this.prazoLimite = prazoLimite;
        this.triadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getIncidenteId() { return incidenteId; }
    public String getTitulo() { return titulo; }
    public Prioridade getPrioridade() { return prioridade; }
    public int getPrazoMinutos() { return prazoMinutos; }
    public Instant getPrazoLimite() { return prazoLimite; }
    public Instant getTriadaEm() { return triadaEm; }
}
