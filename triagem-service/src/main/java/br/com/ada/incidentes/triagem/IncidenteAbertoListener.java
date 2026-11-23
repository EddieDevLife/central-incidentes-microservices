package br.com.ada.incidentes.triagem;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Quem "abre a caixa de correio". Fica só com a parte de receber;
 * a regra de negócio mora no TriagemService.
 *
 * Se processar() lançar exceção, o Spring tenta de novo (3 vezes, ver
 * application.yml) e depois manda a mensagem para a DLQ.
 */
@Component
public class IncidenteAbertoListener {

    private final TriagemService triagemService;

    public IncidenteAbertoListener(TriagemService triagemService) {
        this.triagemService = triagemService;
    }

    @RabbitListener(queues = RabbitConfig.FILA_TRIAGEM)
    public void receber(IncidenteAbertoEvent evento) {
        triagemService.processar(evento);
    }
}
