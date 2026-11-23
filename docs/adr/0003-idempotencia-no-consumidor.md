# ADR 0003: Mensagem repetida não duplica (idempotência no consumidor)

**Status:** aceito

## Contexto

A fila entrega **pelo menos uma vez**. A mesma mensagem pode chegar duas vezes quando, por exemplo:

- o consumidor processa, mas cai antes de confirmar (ack), e o broker entrega de novo;
- quem publica não recebe a confirmação, acha que falhou e publica outra vez.

Sem cuidado, um incidente teria duas triagens.

## Decisão

1. Toda mensagem leva um `eventoId` (UUID), criado junto com o incidente e que **não muda** em reenvios.
2. O `triagem-service` mantém a tabela `mensagens_processadas`, cuja **chave primária é o `eventoId`**.
3. Ao receber uma mensagem, em **uma única transação**:
   - se o `eventoId` já está na tabela, ignora e registra um aviso no log;
   - senão, grava o `eventoId` e a triagem juntos.

Se duas cópias chegarem ao mesmo tempo, as duas passam pelo "já existe?", mas a chave primária barra a segunda gravação. Ela falha, é tentada de novo e, aí sim, é reconhecida como repetida.

Há ainda uma segunda trava: `triagens.incidente_id` é único.

## O que mais foi considerado

| Alternativa | Por que não |
|---|---|
| Só a restrição única em `triagens.incidente_id` | Funciona neste caso, mas mistura "já vi esta mensagem" com uma regra de negócio. Se um dia houver outro tipo de mensagem para o mesmo incidente (por exemplo, "reclassificar"), a regra deixa de servir. |
| Guardar os `eventoId` no Redis (`SETNX`) | O Redis e o Postgres não participam da mesma transação: daria para marcar como vista e falhar ao gravar a triagem, perdendo a mensagem. |
| "Exactly-once" do broker | O RabbitMQ não oferece isso de ponta a ponta. Na prática, o que existe é "pelo menos uma vez" + consumidor idempotente. |

## Consequências

- **Bom:** reenviar a mesma mensagem 1 ou 100 vezes dá o mesmo resultado. Demonstrável com `POST /incidentes/{id}/reenviar`.
- **Custo:** uma leitura e uma gravação a mais por mensagem.
- **Custo:** a tabela `mensagens_processadas` cresce para sempre. Em produção, uma rotina apagaria os registros antigos (por exemplo, com mais de 30 dias).
