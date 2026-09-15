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

# Encerra containers de monitoramento se ativos
./stop-monitoring.sh 2>/dev/null || true

echo "Todos os serviços foram finalizados com sucesso."

