# ADR 0005: Retry e DLQ para mensagens que falham

**Status:** aceito (item além do obrigatório)

## Contexto

O processamento de uma mensagem pode falhar por um motivo passageiro (banco reiniciando) ou definitivo (mensagem malformada). Por padrão, uma mensagem que falha volta para a fila e é entregue de novo imediatamente; se o erro for definitivo, isso vira um laço infinito que trava a fila.

## Decisão

- **Retry:** 3 tentativas, com espera de 1 s e depois 2 s (`spring.rabbitmq.listener.simple.retry`).
- **DLQ:** se as 3 falharem, a mensagem é rejeitada sem voltar para a fila (`default-requeue-rejected: false`). Como a fila principal foi declarada com uma *dead-letter exchange* (`incidentes.dlx`), o RabbitMQ a move para a fila `incidentes.abertos.triagem.dlq`.

Lá ela fica guardada para alguém analisar e, se fizer sentido, reprocessar.

## O que mais foi considerado

| Alternativa | Por que não |
|---|---|
| Devolver para a fila sempre | Laço infinito no caso de erro definitivo. |
| Descartar após as tentativas | Perde o incidente sem deixar rastro. |
| Fila de retry com atraso (TTL + DLX de volta) | Dá esperas maiores entre tentativas sem prender o consumidor, mas é mais configuração do que o projeto precisa. |

## Consequências

- **Bom:** erro passageiro se resolve sozinho; erro definitivo não trava a fila nem some.
- **Custo:** durante as tentativas, o consumidor fica ocupado com aquela mensagem (até cerca de 3 s).
- **Custo:** alguém precisa acompanhar a DLQ (painel do RabbitMQ, aba *Queues*). Não há reprocessamento automático.
- **Ligação com o ADR 0003:** as novas tentativas só são seguras porque o consumidor é idempotente.
