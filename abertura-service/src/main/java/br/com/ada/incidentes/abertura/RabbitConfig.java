package br.com.ada.incidentes.abertura;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Analogia dos Correios:
 *  - exchange    = a agência: você entrega a carta lá, não na casa do destinatário;
 *  - routing key = o CEP escrito na carta;
 *  - fila        = a caixa de correio do destinatário, onde a carta espera.
 *
 * A fila é declarada AQUI também (igualzinha à do triagem-service) para que
 * ela exista mesmo que este serviço suba primeiro. Sem isso, uma mensagem
 * publicada antes de a fila existir seria jogada fora pelo RabbitMQ.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "incidentes.exchange";
    public static final String ROUTING_KEY = "incidente.aberto";
    public static final String FILA_TRIAGEM = "incidentes.abertos.triagem";

    // Para onde vai a mensagem que o consumidor não conseguiu processar (ver ADR 0005)
    public static final String DLX = "incidentes.dlx";
    public static final String DLQ_ROUTING_KEY = "incidente.aberto.falhou";

    @Bean
    public DirectExchange incidentesExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue filaTriagem() {
        return QueueBuilder.durable(FILA_TRIAGEM)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding ligacaoFilaTriagem() {
        return BindingBuilder.bind(filaTriagem()).to(incidentesExchange()).with(ROUTING_KEY);
    }

    /** Faz a mensagem viajar como JSON (legível no painel do RabbitMQ) em vez de bytes Java. */
    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
