package br.com.ada.incidentes.abertura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidentes")
public class Incidente {

    @Id
    private UUID id;

    /**
     * Identificador da MENSAGEM que avisa "este incidente foi aberto".
     * Nasce junto com o incidente e nunca muda: se a mensagem for enviada
     * de novo, ela leva o mesmo eventoId. É por ele que quem recebe
     * descobre que "essa eu já vi".
     */
    @Column(nullable = false, unique = true)
    private UUID eventoId;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nivel impacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nivel urgencia;

    @Column(nullable = false)
    private Instant abertoEm;

    protected Incidente() {
        // exigido pelo JPA
    }

    public Incidente(String titulo, String descricao, Nivel impacto, Nivel urgencia) {
        this.id = UUID.randomUUID();
        this.eventoId = UUID.randomUUID();
        this.titulo = titulo;
        this.descricao = descricao;
        this.impacto = impacto;
        this.urgencia = urgencia;
        this.abertoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventoId() { return eventoId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Nivel getImpacto() { return impacto; }
    public Nivel getUrgencia() { return urgencia; }
    public Instant getAbertoEm() { return abertoEm; }
}
