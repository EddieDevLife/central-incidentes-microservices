package br.com.ada.incidentes.triagem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraPrioridadeTest {

    @Test
    void impactoEUrgenciaAltosDaoP1() {
        assertEquals(Prioridade.P1, CalculadoraPrioridade.calcular(Nivel.ALTO, Nivel.ALTO));
    }

    @Test
    void umAltoEUmMedioDaoP2() {
        assertEquals(Prioridade.P2, CalculadoraPrioridade.calcular(Nivel.ALTO, Nivel.MEDIO));
        assertEquals(Prioridade.P2, CalculadoraPrioridade.calcular(Nivel.MEDIO, Nivel.ALTO));
    }

    @Test
    void somaQuatroDaP3() {
        assertEquals(Prioridade.P3, CalculadoraPrioridade.calcular(Nivel.MEDIO, Nivel.MEDIO));
        assertEquals(Prioridade.P3, CalculadoraPrioridade.calcular(Nivel.ALTO, Nivel.BAIXO));
    }

    @Test
    void oRestoDaP4() {
        assertEquals(Prioridade.P4, CalculadoraPrioridade.calcular(Nivel.MEDIO, Nivel.BAIXO));
        assertEquals(Prioridade.P4, CalculadoraPrioridade.calcular(Nivel.BAIXO, Nivel.BAIXO));
    }
}
