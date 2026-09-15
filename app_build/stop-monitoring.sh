#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=================================================="
echo "Parando Containers de Monitoramento (Prometheus e Grafana)"
echo "=================================================="

if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    echo "ERRO: docker compose não foi encontrado."
    exit 1
fi

$COMPOSE_CMD down

echo "Containers de monitoramento finalizados com sucesso."
