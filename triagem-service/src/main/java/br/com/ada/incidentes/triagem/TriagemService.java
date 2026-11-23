package br.com.ada.incidentes.triagem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class TriagemService {

    private static final Logger log = LoggerFactory.getLogger(TriagemService.class);

    private final MensagemProcessadaRepository mensagensProcessadas;
    private final TriagemRepository triagens;
    private final RegraSlaService regrasSla;

    public TriagemService(MensagemProcessadaRepository mensagensProcessadas,
                          TriagemRepository triagens,
                          RegraSlaService regrasSla) {
        this.mensagensProcessadas = mensagensProcessadas;
        this.triagens = triagens;
        this.regrasSla = regrasSla;
    }

    /**
     * IDEMPOTÊNCIA: processar a mesma mensagem 1 ou 10 vezes dá o mesmo resultado.
     *
     * Analogia: a portaria do prédio com um caderninho de encomendas.
     *   1. Chegou um pacote: o porteiro olha o código de rastreio no caderninho.
     *   2. Já está anotado? Então é repetido: descarta e segue a vida.
     *   3. Não está? Anota o código E entrega o pacote, as duas coisas juntas.
     *
     * "As duas coisas juntas" é o @Transactional: a anotação no caderninho
     * (mensagens_processadas) e a triagem são gravadas na mesma transação.
     * Ou entram as duas, ou nenhuma. Nunca fica "anotado mas não entregue".
     */
    @Transactional
    public void processar(IncidenteAbertoEvent evento) {

        // Passo 1 e 2: já vi esta mensagem?
        if (mensagensProcessadas.existsById(evento.eventoId())) {
            log.warn("Mensagem repetida ignorada: eventoId={} incidenteId={}",
                    evento.eventoId(), evento.incidenteId());
            return;
        }

        // Passo 3a: anota no caderninho. Como eventoId é chave primária, se duas
        // cópias chegarem no mesmo instante, o banco barra a segunda aqui: ela
        // falha, é tentada de novo e, na nova tentativa, cai no "já vi" acima.
        mensagensProcessadas.saveAndFlush(new MensagemProcessada(evento.eventoId()));

        // Passo 3b: o trabalho de verdade.
        Prioridade prioridade = CalculadoraPrioridade.calcular(evento.impacto(), evento.urgencia());
        RegraSlaDto regra = regrasSla.buscar(prioridade);   // <- passa pelo cache
        Instant prazoLimite = evento.abertoEm().plus(Duration.ofMinutes(regra.prazoMinutos()));

        triagens.save(new Triagem(
                evento.incidenteId(), evento.titulo(), prioridade, regra.prazoMinutos(), prazoLimite));

        log.info("Incidente triado: incidenteId={} prioridade={} prazoLimite={}",
                evento.incidenteId(), prioridade, prazoLimite);
    }
}
