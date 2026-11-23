# ADR 0002: RabbitMQ como fila entre os serviços

**Status:** aceito

## Contexto

O `abertura-service` precisa avisar o `triagem-service` de que há um incidente novo. Quem abre o incidente não precisa esperar a triagem terminar.

## Decisão

Comunicação assíncrona por uma fila no RabbitMQ:

- o `abertura-service` publica a mensagem `IncidenteAberto` (JSON) na exchange `incidentes.exchange` com a routing key `incidente.aberto`;
- a fila durável `incidentes.abertos.triagem` recebe essas mensagens;
- o `triagem-service` consome a fila.

Os serviços não compartilham código: cada um tem a sua classe `IncidenteAbertoEvent`. O contrato é o formato do JSON.

A gravação do incidente e a publicação acontecem dentro da mesma transação do banco: se a publicação falhar, a gravação é desfeita.

## O que mais foi considerado

| Alternativa | Por que não |
|---|---|
| Chamada HTTP direta (síncrona) | Acopla os dois: se a triagem estiver fora, a abertura falha ou fica lenta. |
| Kafka | Ótimo para volume alto e para reler o histórico, mas é mais pesado de operar e de explicar; para "uma tarefa que um consumidor executa" uma fila simples basta. |
| Tabela no banco servindo de fila | Exigiria os dois serviços no mesmo banco, o que contraria o ADR 0001. |

## Consequências

- **Bom:** a abertura responde rápido e não depende de a triagem estar no ar.
- **Bom:** picos de incidentes são absorvidos pela fila.
- **Custo:** o RabbitMQ garante entrega **pelo menos uma vez**, então a mesma mensagem pode chegar repetida. Tratado no ADR 0003.
- **Limitação conhecida:** se o banco falhar exatamente no commit, depois de a mensagem já ter sido publicada, sobra uma mensagem sem incidente gravado. A solução completa é o padrão *Transactional Outbox* (gravar a mensagem em uma tabela na mesma transação e publicá-la depois). Ficou fora do escopo por simplicidade.
