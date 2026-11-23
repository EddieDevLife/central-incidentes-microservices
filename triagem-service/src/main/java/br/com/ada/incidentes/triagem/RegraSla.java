package br.com.ada.incidentes.triagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Regra de SLA: quantos minutos há para resolver um incidente de cada prioridade. */
@Entity
@Table(name = "regras_sla")
public class RegraSla {

    @Id
    private String prioridade;   // "P1", "P2", "P3" ou "P4"

    @Column(nullable = false)
    private int prazoMinutos;

    protected RegraSla() {
    }

    public RegraSla(String prioridade, int prazoMinutos) {
        this.prioridade = prioridade;
        this.prazoMinutos = prazoMinutos;
    }

    public String getPrioridade() { return prioridade; }
    public int getPrazoMinutos() { return prazoMinutos; }
    public void setPrazoMinutos(int prazoMinutos) { this.prazoMinutos = prazoMinutos; }
}
