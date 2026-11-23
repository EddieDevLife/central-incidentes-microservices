package br.com.ada.incidentes.triagem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes sem Spring, sem banco e sem fila: só a regra de negócio. */
@ExtendWith(MockitoExtension.class)
class TriagemServiceTest {

    @Mock
    MensagemProcessadaRepository mensagensProcessadas;

    @Mock
    TriagemRepository triagens;

    @Mock
    RegraSlaService regrasSla;

    @InjectMocks
    TriagemService service;

    private final IncidenteAbertoEvent evento = new IncidenteAbertoEvent(
            UUID.randomUUID(), UUID.randomUUID(), "Internet banking fora do ar",
            Nivel.ALTO, Nivel.ALTO, Instant.parse("2026-11-16T12:00:00Z"));

    @Test
    void mensagemRepetidaNaoGravaNada() {
        when(mensagensProcessadas.existsById(evento.eventoId())).thenReturn(true);

        service.processar(evento);

        verify(mensagensProcessadas, never()).saveAndFlush(any());
        verify(triagens, never()).save(any());
    }

    @Test
    void mensagemNovaGravaTriagemComOPrazoDaRegra() {
        when(mensagensProcessadas.existsById(evento.eventoId())).thenReturn(false);
        when(regrasSla.buscar(Prioridade.P1)).thenReturn(new RegraSlaDto("P1", 60));

        service.processar(evento);

        verify(mensagensProcessadas).saveAndFlush(any(MensagemProcessada.class));

        ArgumentCaptor<Triagem> gravada = ArgumentCaptor.forClass(Triagem.class);
        verify(triagens).save(gravada.capture());
        assertEquals(evento.incidenteId(), gravada.getValue().getIncidenteId());
        assertEquals(Prioridade.P1, gravada.getValue().getPrioridade());
        assertEquals(evento.abertoEm().plus(Duration.ofMinutes(60)), gravada.getValue().getPrazoLimite());
    }
}
