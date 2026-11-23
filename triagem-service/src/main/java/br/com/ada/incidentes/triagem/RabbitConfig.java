package br.com.ada.incidentes.triagem;

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
 * Os nomes e os argumentos da fila principal precisam ser IDÊNTICOS aos do
 * abertura-service. Se um lado declarar a fila de um jeito diferente, o
 * RabbitMQ recusa a declaração.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "incidentes.exchange";
    public static final String ROUTING_KEY = "incidente.aberto";
    public static final String FILA_TRIAGEM = "incidentes.abertos.triagem";

    // "Achados e perdidos": para onde vai a mensagem que falhou 3 vezes.
    public static final String DLX = "incidentes.dlx";
    public static final String DLQ = "incidentes.abertos.triagem.dlq";
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

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue filaDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding ligacaoDlq() {
        return BindingBuilder.bind(filaDlq()).to(dlxExchange()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
