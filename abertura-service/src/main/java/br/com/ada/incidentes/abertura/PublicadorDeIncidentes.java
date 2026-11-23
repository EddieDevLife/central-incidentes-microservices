package br.com.ada.incidentes.abertura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** O único lugar deste serviço que fala com a fila. */
@Component
public class PublicadorDeIncidentes {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeIncidentes.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorDeIncidentes(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(Incidente incidente) {
        IncidenteAbertoEvent evento = new IncidenteAbertoEvent(
                incidente.getEventoId(),
                incidente.getId(),
                incidente.getTitulo(),
                incidente.getImpacto(),
                incidente.getUrgencia(),
                incidente.getAbertoEm());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, evento);

        log.info("Evento publicado na fila: eventoId={} incidenteId={}",
                evento.eventoId(), evento.incidenteId());
    }
}
