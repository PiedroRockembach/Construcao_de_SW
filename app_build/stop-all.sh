#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "Finalizando todos os microsserviços..."

for pidfile in logs/*.pid; do
    if [ -f "$pidfile" ]; then
        pid=$(cat "$pidfile")
        if kill -0 "$pid" 2>/dev/null; then
            echo "Encerrando processo $pid ($(basename "$pidfile" .pid))..."
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$pidfile"
    fi
done

# Fallback para liberar portas se necessário
for port in 8888 8761 8080 8081 8082 8083; do
    fuser -k "${port}/tcp" 2>/dev/null || true
done

# Stack de Observabilidade (dados preservados nos volumes Docker)
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    echo "Encerrando Prometheus e Grafana..."
    docker compose -f observability/docker-compose.yml down || true
fi

echo "Todos os serviços foram finalizados com sucesso."
