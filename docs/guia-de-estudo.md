# Guia de estudo: Central de Incidentes

Este guia é para **você** entender e conseguir explicar o projeto na banca. A regra do método Feynman: se você não consegue explicar uma parte com palavras simples, ainda não a entendeu. Cada seção termina com um "explique com as suas palavras" para você treinar em voz alta.

---

## 1. A história inteira em 30 segundos

Pense em um **pronto-socorro**:

- a **recepção** anota quem chegou e o que está sentindo. Ela não decide a gravidade; só registra, e rápido;
- a ficha vai para um **escaninho**;
- a **enfermagem de triagem** pega as fichas do escaninho, classifica a gravidade (a pulseira colorida) e define em quanto tempo a pessoa precisa ser atendida;
- a tabela "cor da pulseira → tempo máximo" está em um **cartaz na parede**, para ninguém precisar buscar o manual no arquivo a cada paciente.

| No pronto-socorro | No projeto |
|---|---|
| Recepção | `abertura-service` |
| Livro de registro da recepção | banco `abertura` |
| Escaninho | fila no RabbitMQ |
| Enfermagem de triagem | `triagem-service` |
| Prontuário da triagem | banco `triagem` |
| Cartaz na parede | cache no Redis |
| Manual no arquivo | tabela `regras_sla` |
| Pulseira colorida | prioridade P1 a P4 |

**Explique com as suas palavras:** o que acontece, do começo ao fim, quando alguém faz `POST /incidentes`?

---

## 2. Dois serviços, cada um com o seu banco (25 pontos)

**A ideia simples:** cada serviço é dono dos seus dados, e ninguém mexe na gaveta do outro.

**Analogia:** dois departamentos de uma empresa. Se o Financeiro quiser um dado do RH, ele *pede* ao RH; não entra na sala do RH para abrir o arquivo. Assim o RH pode reorganizar o arquivo quando quiser.

**No código:**

- `docker-compose.yml` tem **dois** Postgres: `abertura-db` (porta 5433) e `triagem-db` (porta 5434);
- cada `application.yml` aponta só para o seu;
- o `triagem-service` guarda apenas o `incidenteId` e o que veio na mensagem. Ele nunca consulta a tabela `incidentes`.

**O que se ganha:**

- se a triagem cair, a abertura continua funcionando;
- cada serviço altera as suas tabelas sem quebrar o outro.

**O que se paga:**

- não dá para fazer `JOIN` entre incidente e triagem;
- **consistência eventual:** a triagem aparece um instante depois da abertura. Por isso `GET /triagens/{id}` pode devolver 404 logo após o `POST`.

**Como provar na banca:** mostre os dois bancos no `docker-compose.yml` e diga: "o triagem-service não tem nem a senha do banco da abertura".

**Explique com as suas palavras:** por que dois serviços usando o mesmo banco não estão separados de verdade?

---

## 3. A fila (25 pontos)

### 3.1 Por que fila em vez de chamada direta

**Analogia:** telefonema x mensagem de WhatsApp.

- **Telefonema (HTTP síncrono):** se a outra pessoa não atender, você fica travado.
- **Mensagem (fila):** você envia e segue a vida; a pessoa lê quando puder. Se ela estiver sem bateria, as mensagens ficam esperando.

A abertura não precisa esperar a triagem. Então ela "manda mensagem".

### 3.2 As peças do RabbitMQ

Analogia dos Correios:

- **Exchange** (`incidentes.exchange`): a agência. Você entrega a carta na agência, não na casa do destinatário.
- **Routing key** (`incidente.aberto`): o CEP escrito na carta.
- **Fila** (`incidentes.abertos.triagem`): a caixa de correio onde a carta espera.
- **Binding:** a regra "cartas com este CEP vão para esta caixa".

**Quem manda:** `PublicadorDeIncidentes` (abertura-service).
**Quem recebe:** `IncidenteAbertoListener` → `TriagemService` (triagem-service).

### 3.3 Mensagem repetida: o ponto que mais vale

**Por que uma mensagem chegaria duas vezes?** A fila garante entrega **pelo menos uma vez**. Exemplo: o consumidor grava a triagem, mas cai antes de avisar "recebi" (ack). O RabbitMQ, sem a confirmação, entrega de novo. Do ponto de vista dele, é o certo a fazer: melhor repetir do que perder.

**Idempotência** é uma palavra difícil para uma ideia simples: fazer a mesma coisa duas vezes dá o mesmo resultado que fazer uma.

- Apertar o botão do elevador cinco vezes: o elevador vem uma vez só. **Idempotente.**
- Apertar "transferir R$ 100" cinco vezes sem proteção: saem R$ 500. **Não idempotente.**

**Como o projeto faz: o caderninho da portaria.**

1. Cada mensagem tem um código de rastreio, o `eventoId`. Ele nasce com o incidente e não muda em reenvios.
2. O porteiro (`TriagemService.processar`) olha o caderninho (tabela `mensagens_processadas`).
3. Código já anotado? Pacote repetido: ignora.
4. Não anotado? Anota **e** entrega, as duas coisas na mesma transação (`@Transactional`). Ou acontecem as duas, ou nenhuma.

**E se duas cópias chegarem no mesmo milissegundo?** As duas olham o caderninho e não acham nada. Só que `eventoId` é **chave primária**: o banco deixa uma anotar e barra a outra. A barrada falha, é tentada de novo e, dessa vez, encontra a anotação e é ignorada.

**Como provar na banca:**

```bash
curl -s -X POST localhost:8081/incidentes/<id>/reenviar   # mesma mensagem de novo
curl -s localhost:8082/triagens                           # continua uma triagem só
docker compose logs triagem-service | grep "repetida"     # o serviço percebeu
```

### 3.4 Retry e DLQ (extra)

- **Retry:** deu erro? Tenta mais duas vezes, com uma pequena espera. Resolve falhas passageiras.
- **DLQ (dead-letter queue):** o "achados e perdidos". A mensagem que falhou três vezes vai para uma fila separada, em vez de travar a fila principal ou sumir.

**Explique com as suas palavras:** por que não basta confiar que a fila entrega cada mensagem uma vez só?

---

## 4. O cache (25 pontos)

**A ideia simples:** guardar por perto a resposta de uma pergunta que se repete muito e muda pouco.

**Analogia:** o post-it no monitor com o ramal que você mais usa. Em vez de abrir a lista telefônica (banco) toda vez, você olha o post-it (Redis).

A banca vai fazer três perguntas. Tenha as respostas na ponta da língua:

| Pergunta | Resposta |
|---|---|
| **O que guarda?** | A regra de SLA de cada prioridade. Chave `regras-sla::P1`, valor `{"prioridade":"P1","prazoMinutos":60}`. |
| **Por quanto tempo?** | 10 minutos (TTL). |
| **Quando apaga?** | Quando alguém altera a regra (`PUT /regras-sla/P1` apaga só a chave de P1) ou quando os 10 minutos vencem. |

**Por que as regras de SLA são boas candidatas?** São lidas a cada incidente e alteradas quase nunca. Cache vale a pena quando há **muita leitura e pouca escrita**.

**Como funciona (cache-aside):**

1. `@Cacheable`: antes de entrar no método `buscar`, o Spring olha o Redis. Achou (*hit*)? Devolve e o método nem roda. Não achou (*miss*)? Roda o método, que vai ao banco, e guarda o resultado no Redis.
2. `@CacheEvict`: ao alterar a regra, grava no banco e rasga o post-it. O próximo que perguntar escreve um novo.

**Por que TTL, se já apago na alteração?** É o cinto de segurança. Se alguém alterar a regra direto no banco, ou se o apagar falhar, o post-it errado dura no máximo 10 minutos em vez de para sempre.

**E se o Redis cair?** O serviço continua: registra um aviso e busca no banco (`CacheConfig.errorHandler`). Cache é atalho, não fonte da verdade.

**Como provar na banca:**

```bash
docker compose exec redis redis-cli KEYS 'regras-sla*'      # o que guarda
docker compose exec redis redis-cli TTL 'regras-sla::P1'    # por quanto tempo (segundos restantes)
curl -s -X PUT localhost:8082/regras-sla/P1 -H 'Content-Type: application/json' -d '{"prazoMinutos":30}'
docker compose exec redis redis-cli KEYS 'regras-sla*'      # quando apaga: P1 sumiu
```

**Explique com as suas palavras:** o que acontece na primeira e na segunda vez que o serviço precisa da regra de P1?

---

## 5. Os ADRs (25 pontos)

**A ideia simples:** um ADR é um bilhete para o seu "eu do futuro" (ou para um colega novo) respondendo: *o que escolhi, o que mais considerei e por quê*.

| ADR | Decisão em uma frase | Alternativa mais forte |
|---|---|---|
| 0001 | Dois serviços, um Postgres para cada | Banco compartilhado |
| 0002 | RabbitMQ entre os serviços | HTTP direto; Kafka |
| 0003 | Tabela de mensagens processadas com `eventoId` como chave | Só a restrição única no incidente; Redis |
| 0004 | Regras de SLA no Redis, TTL de 10 min, apaga na alteração | Cache em memória; sem TTL |
| 0005 | 3 tentativas e depois DLQ | Devolver para a fila sempre |

Não decore. Leia cada um e veja se você **concorda**. Se discordar de algo, altere o ADR: a nota é pela escolha explicada, não por uma escolha "certa".

---

## 6. Roteiro da banca (10 minutos, com as perguntas)

Deixe o `docker compose up --build` **rodando antes** de a sua vez começar.

| Tempo | O que fazer | O que falar |
|---|---|---|
| 0:00 a 1:00 | Mostrar o desenho do README | "Central de Incidentes, perfil A. Um serviço registra o incidente, uma fila avisa o outro, que calcula a prioridade e o prazo." |
| 1:00 a 2:00 | `docker-compose.yml` | "Dois serviços, dois bancos. Um não tem acesso ao banco do outro." |
| 2:00 a 4:30 | Rodar `./scripts/demo.sh`, passos 1 a 3 | "Abri o incidente, a triagem chegou pela fila. Reenviei a mesma mensagem duas vezes: continua uma triagem só, e o log mostra que foi ignorada." |
| 4:30 a 6:00 | Passos 4 a 6 do script | "O cache guarda a regra de SLA por 10 minutos. Alterei a regra: a chave sumiu. Consultei de novo: uma ida ao banco, depois cache." |
| 6:00 a 7:00 | Abrir a pasta `docs/adr` | "Cinco decisões. A mais importante é a 0003, da idempotência." Diga uma alternativa descartada. |
| 7:00 a 7:30 | Uso de IA | Ver a seção 7. |
| 7:30 a 10:00 | Perguntas | |

### Perguntas prováveis

- **"E se o triagem-service estiver fora do ar?"** As mensagens esperam na fila (ela é durável). Quando ele volta, processa tudo. Dá para demonstrar: `docker compose stop triagem-service`, abrir um incidente, `docker compose start triagem-service`.
- **"E se o RabbitMQ estiver fora do ar?"** A abertura devolve erro e desfaz a gravação: não fica incidente sem aviso.
- **"E se gravar no banco e a publicação falhar pela metade?"** Limitação assumida no ADR 0002; a solução completa seria o padrão Outbox.
- **"Por que RabbitMQ e não Kafka?"** É uma tarefa para um consumidor executar, sem necessidade de reler histórico. Fila simples resolve.
- **"Por que não usar o Redis para a idempotência?"** Porque o Redis e o Postgres não entram na mesma transação: poderia marcar como vista e falhar ao gravar.
- **"O cache pode devolver um valor velho?"** Só se a regra for alterada por fora da API ou se o apagar falhar, e por no máximo 10 minutos.
- **"Como o triagem-service sabe os dados do incidente?"** Só pelo que vem na mensagem. Ele não consulta o outro serviço nem o outro banco.
- **"Mudar a regra de SLA muda os incidentes já triados?"** Não. Vale para os próximos.

---

## 7. Uso de IA: o que conferir à mão

O enunciado pede que você conte se usou IA e o que conferiu. Seja direto: "o código e os documentos foram gerados com IA; eu conferi o seguinte". Marque cada item **só depois de fazer**:

- [ ] Rodei `docker compose up --build` na minha máquina e os dois serviços subiram.
- [ ] Rodei `./scripts/demo.sh` e conferi cada saída com o que este guia diz que deveria aparecer.
- [ ] Vi a mensagem em JSON no painel do RabbitMQ (pare o `triagem-service`, abra um incidente e use *Get messages* na fila).
- [ ] Conferi no banco que há uma triagem só após os reenvios:
      `docker compose exec triagem-db psql -U triagem -d triagem -c "select incidente_id, prioridade from triagens;"`
- [ ] Conferi no banco que o `abertura-db` não tem as tabelas da triagem, e vice-versa (`\dt` no `psql`).
- [ ] Vi a chave e o TTL no Redis, e vi a chave sumir após o `PUT`.
- [ ] Li `TriagemService.processar` linha a linha e sei explicar cada uma.
- [ ] Li os cinco ADRs e concordo com eles (ou alterei o que discordava).
- [ ] Rodei `mvn test` no `triagem-service` (se tiver Java 25 e Maven).

---

## 8. Sugestão de commits (um por aula, como pede o enunciado)

O enunciado pede commits aos poucos. Uma ordem que conta a história do projeto:

1. **Tema, desenho e esqueleto:** `README.md` (o que faz e o desenho), `docker-compose.yml`, os dois `pom.xml`, as classes `*Application`, entidades, repositórios, controllers e o ADR 0001.
2. **A fila:** os `RabbitConfig`, `PublicadorDeIncidentes`, `IncidenteAbertoEvent`, `IncidenteAbertoListener`, `TriagemService`, `MensagemProcessada*` e os ADRs 0002, 0003 e 0005.
3. **O cache:** `CacheConfig`, `RegraSlaService`, `RegraSlaController` e o ADR 0004.
4. **Fechamento:** testes, `scripts/demo.sh`, este guia e os ajustes finais do README.

---

## 9. Mini-glossário

- **Assíncrono:** quem pede não fica esperando a resposta.
- **Broker:** o programa que cuida das filas (aqui, o RabbitMQ).
- **Ack:** o "recebi e processei" que o consumidor dá ao broker.
- **Pelo menos uma vez (*at-least-once*):** a mensagem não se perde, mas pode repetir.
- **Idempotente:** repetir não muda o resultado.
- **Consistência eventual:** os dados dos dois serviços ficam de acordo, mas não no mesmo instante.
- **TTL:** prazo de validade de um item no cache.
- **Cache hit / miss:** achou / não achou no cache.
- **Evict:** apagar um item do cache.
- **DLQ:** fila para onde vão as mensagens que não puderam ser processadas.
- **ADR:** registro curto de uma decisão de arquitetura.
