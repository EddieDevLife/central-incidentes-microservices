#!/usr/bin/env bash
# Roteiro da demonstração (o mesmo da banca).
# Antes: docker compose up --build   (espere os dois serviços subirem)
# Uso:   ./scripts/demo.sh

set -euo pipefail

ABERTURA=http://localhost:8081
TRIAGEM=http://localhost:8082

titulo() { printf '\n\n===== %s =====\n' "$1"; }

titulo "1. Abrindo um incidente (abertura-service grava no SEU banco e publica na fila)"
RESPOSTA=$(curl -s -X POST "$ABERTURA/incidentes" \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Internet banking fora do ar","descricao":"Clientes nao conseguem logar","impacto":"ALTO","urgencia":"ALTO"}')
echo "$RESPOSTA"
ID=$(echo "$RESPOSTA" | sed -E 's/.*"id":"([^"]+)".*/\1/')
echo "ID do incidente: $ID"

sleep 2

titulo "2. Consultando a triagem (triagem-service recebeu pela fila e gravou no banco DELE)"
curl -s "$TRIAGEM/triagens/$ID"

titulo "3. Mandando a MESMA mensagem mais duas vezes"
curl -s -o /dev/null -X POST "$ABERTURA/incidentes/$ID/reenviar"
curl -s -o /dev/null -X POST "$ABERTURA/incidentes/$ID/reenviar"
sleep 2
echo "Triagens gravadas para este incidente (tem que ser 1):"
curl -s "$TRIAGEM/triagens" | grep -o "\"incidenteId\":\"$ID\"" | wc -l
echo "O que o triagem-service registrou:"
docker compose logs triagem-service | grep "Mensagem repetida ignorada" | tail -2 || true

titulo "4. Cache: a regra de SLA de P1 está no Redis, com prazo de validade"
echo "Chaves no Redis:"
docker compose exec -T redis redis-cli KEYS 'regras-sla*'
echo "Segundos até vencer (TTL):"
docker compose exec -T redis redis-cli TTL 'regras-sla::P1'

titulo "5. Alterando a regra de P1 (o cache dessa regra é apagado)"
curl -s -X PUT "$TRIAGEM/regras-sla/P1" \
  -H 'Content-Type: application/json' -d '{"prazoMinutos":30}'
echo
echo "Chaves no Redis depois da alteração (P1 sumiu):"
docker compose exec -T redis redis-cli KEYS 'regras-sla*'

titulo "6. Consultando de novo: 1ª vez vai ao banco (CACHE MISS), 2ª vem do cache"
curl -s "$TRIAGEM/regras-sla/P1"; echo
curl -s "$TRIAGEM/regras-sla/P1"; echo
echo "Idas ao banco para P1 desde que o servico subiu (esperado nesta demo: 2 = a da triagem + a de depois da alteracao):"
docker compose logs triagem-service | grep -c "CACHE MISS: buscando regra de SLA de P1" || true

titulo "Fim. Voltando a regra de P1 para 60 minutos"
curl -s -X PUT "$TRIAGEM/regras-sla/P1" \
  -H 'Content-Type: application/json' -d '{"prazoMinutos":60}'
echo
