# ADR 0001: Dois serviços, um banco para cada

**Status:** aceito

## Contexto

Abrir um incidente e fazer a triagem dele são trabalhos diferentes. A abertura precisa responder rápido e estar sempre disponível (ninguém pode ficar sem conseguir registrar um problema). A triagem tem regras que mudam por outros motivos (matriz de prioridade, prazos de SLA) e pode levar alguns segundos sem prejuízo.

## Decisão

Dois serviços, cada um com o seu Postgres:

- `abertura-service` + banco `abertura` (tabela `incidentes`);
- `triagem-service` + banco `triagem` (tabelas `triagens`, `regras_sla`, `mensagens_processadas`).

Nenhum serviço lê ou grava no banco do outro. O `triagem-service` guarda só o `incidenteId` e os poucos dados que chegam na mensagem.

## O que mais foi considerado

| Alternativa | Por que não |
|---|---|
| Um serviço só (monolito) | Mais simples, mas a abertura cairia junto se a triagem tivesse um erro, e o exercício pede separação. |
| Dois serviços, um banco compartilhado | Parece separado, mas não é: uma mudança de tabela de um quebra o outro, e nenhum pode ser implantado sozinho. |
| Dois *schemas* no mesmo Postgres | Meio-termo aceitável, porém dois containers deixam a separação visível e impossível de burlar com um `JOIN`. |

## Consequências

- **Bom:** se o `triagem-service` cair, continua sendo possível abrir incidentes; eles ficam esperando na fila.
- **Bom:** cada serviço muda as suas tabelas sem combinar com o outro.
- **Custo:** não existe `JOIN` entre incidente e triagem. Quem quiser os dois faz duas chamadas.
- **Custo:** a triagem aparece um instante depois da abertura (consistência eventual). Um `GET /triagens/{id}` logo após o `POST` pode responder 404.
