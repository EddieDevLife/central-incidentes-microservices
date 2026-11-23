# ADR 0004: Cache das regras de SLA no Redis

**Status:** aceito

## Contexto

Para cada incidente, a triagem precisa da regra de SLA da prioridade calculada ("P1 = 60 minutos"). Essas regras são **lidas o tempo todo e alteradas raramente**: o caso em que um cache mais ajuda.

## Decisão

| Pergunta | Resposta |
|---|---|
| **O que guarda?** | A regra de SLA de cada prioridade, em JSON. Chaves `regras-sla::P1` ... `regras-sla::P4`. |
| **Por quanto tempo?** | 10 minutos (TTL), configurável em `app.cache.regras-sla-ttl-minutos`. |
| **Quando apaga?** | Quando a regra é alterada (`PUT /regras-sla/{prioridade}` apaga só a chave daquela prioridade) ou quando o TTL vence, o que vier primeiro. |

Estratégia *cache-aside*: procura no cache; se não achar, busca no banco e guarda no cache (`@Cacheable`). Na alteração, grava no banco e depois apaga a chave (`@CacheEvict`).

Se o Redis estiver fora do ar, o erro é registrado no log e a consulta segue direto para o banco: o cache é um atalho, não uma dependência.

## O que mais foi considerado

| Alternativa | Por que não |
|---|---|
| Sem cache | Funcionaria (a tabela é pequena), mas é uma ida ao banco por mensagem para ler algo que quase nunca muda. |
| Cache em memória dentro do serviço (Caffeine) | Mais rápido e sem infraestrutura, mas com duas instâncias do serviço cada uma teria a sua cópia, e alterar a regra em uma não apagaria o cache da outra. |
| Guardar o resultado da triagem no cache | O resultado é gravado uma vez e consultado pouco; o ganho seria menor do que o das regras. |
| Sem TTL, só apagar na alteração | Se alguém alterar a regra direto no banco, ou se o apagar falhar, o valor antigo ficaria para sempre. O TTL limita esse erro a 10 minutos. |
| Atualizar o cache na alteração (`@CachePut`) em vez de apagar | Apagar é mais simples e seguro: o próximo leitor sempre recarrega do banco, que é a fonte da verdade. |

## Consequências

- **Bom:** a maioria das triagens não consulta a tabela `regras_sla`.
- **Custo:** na pior hipótese (alteração por fora da API, ou Redis indisponível bem na hora de apagar), uma regra antiga pode ser usada por até 10 minutos. Aceitável para prazos de SLA.
- **Observação:** alterar a regra não recalcula as triagens já feitas; vale para os próximos incidentes.
