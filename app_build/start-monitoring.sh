#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=================================================="
echo "Iniciando Containers: Prometheus e Grafana"
echo "=================================================="

# Detecta docker compose ou docker-compose
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    echo "ERRO: docker compose não foi encontrado. Instale o Docker e o plugin compose."
    exit 1
fi

$COMPOSE_CMD up -d

echo "Aguardando Prometheus (porta 9090)..."
for i in {1..30}; do
    if curl -s http://localhost:9090/-/healthy | grep -q "Prometheus Server is Healthy"; then
        echo "Prometheus está pronto em http://localhost:9090"
        break
    fi
    sleep 1
done

echo "Aguardando Grafana (porta 3000)..."
for i in {1..30}; do
    if curl -s http://localhost:3000/api/health | grep -q "ok"; then
        echo "Grafana está pronto em http://localhost:3000"
        break
    fi
    sleep 1
done

echo "=================================================="
echo "Observabilidade em Execução!"
echo "Prometheus: http://localhost:9090"
echo "Grafana:    http://localhost:3000 (Login: admin / admin)"
echo "Dashboard:  http://localhost:3000/d/spring-microservices"
echo "=================================================="
