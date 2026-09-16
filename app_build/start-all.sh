#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

mkdir -p logs

echo "=================================================="
echo "Iniciando Ecossistema de Microserviços Spring Cloud"
echo "=================================================="

# Limpa processos anteriores
for port in 8888 8761 8080 8081 8082 8083; do
    fuser -k "${port}/tcp" 2>/dev/null || true
done
sleep 1

# 0. Stack de Observabilidade (Prometheus + Grafana) - opcional, requer Docker
OBS_UP=0
if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
    echo "[0/4] AVISO: Docker/Compose nao encontrado - Prometheus e Grafana NAO serao iniciados."
elif ! docker info >/dev/null 2>&1; then
    echo "[0/4] AVISO: o Docker esta instalado, mas este usuario nao consegue falar com o daemon."
    echo "       Prometheus e Grafana NAO serao iniciados. Possiveis causas:"
    echo "       - usuario fora do grupo 'docker':  sudo usermod -aG docker \"$USER\" && newgrp docker"
    echo "       - daemon parado:                   sudo systemctl start docker"
    echo "       Alternativa imediata: sudo docker compose -f observability/docker-compose.yml up -d"
else
    echo "[0/4] Iniciando Prometheus (9090) e Grafana (3000) via Docker Compose..."
    if docker compose -f observability/docker-compose.yml up -d; then
        OBS_UP=1
    else
        echo "[0/4] AVISO: falha ao subir a stack de observabilidade (veja o erro acima)."
    fi
fi

# 1. Config Server
echo "[1/4] Iniciando Config Server (Porta 8888)..."
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar > logs/config-server.log 2>&1 &
CONFIG_PID=$!
echo $CONFIG_PID > logs/config-server.pid

echo "Aguardando Config Server inicializar..."
for i in {1..35}; do
    if curl -s http://localhost:8888/actuator/health | grep -q "UP"; then
        echo "Config Server está UP!"
        break
    fi
    sleep 2
done

# 2. Discovery Server (Eureka)
echo "[2/4] Iniciando Discovery Server (Eureka - Porta 8761)..."
java -jar discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar > logs/discovery-server.log 2>&1 &
DISCOVERY_PID=$!
echo $DISCOVERY_PID > logs/discovery-server.pid

echo "Aguardando Discovery Server inicializar..."
for i in {1..35}; do
    if curl -s http://localhost:8761/actuator/health | grep -q "UP"; then
        echo "Discovery Server (Eureka) está UP!"
        break
    fi
    sleep 2
done

# 3. Microserviços de Negócio
echo "[3/4] Iniciando pecas-service, clientes-service e representantes-service..."
java -jar pecas-service/target/pecas-service-1.0.0-SNAPSHOT.jar > logs/pecas-service.log 2>&1 &
PECAS_PID=$!
echo $PECAS_PID > logs/pecas-service.pid

java -jar clientes-service/target/clientes-service-1.0.0-SNAPSHOT.jar > logs/clientes-service.log 2>&1 &
CLIENTES_PID=$!
echo $CLIENTES_PID > logs/clientes-service.pid

java -jar representantes-service/target/representantes-service-1.0.0-SNAPSHOT.jar > logs/representantes-service.log 2>&1 &
REP_PID=$!
echo $REP_PID > logs/representantes-service.pid

echo "Aguardando inicialização dos 3 microsserviços..."
sleep 12

# 4. API Gateway
echo "[4/4] Iniciando API Gateway (Porta 8080)..."
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar > logs/gateway-service.log 2>&1 &
GATEWAY_PID=$!
echo $GATEWAY_PID > logs/gateway-service.pid

echo "Aguardando API Gateway inicializar na porta 8080..."
for i in {1..35}; do
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo "API Gateway está UP!"
        break
    fi
    sleep 2
done

echo "=================================================="
echo "Todos os serviços foram iniciados com sucesso!"
echo "Ponto de entrada único (Gateway): http://localhost:8080"
echo "Eureka Dashboard: http://localhost:8761"
echo "Config Server: http://localhost:8888"
if [ "$OBS_UP" = "1" ]; then
    echo "Prometheus: http://localhost:9090 (targets: http://localhost:9090/targets)"
    echo "Grafana: http://localhost:3000 (admin/admin) - dashboard 'Microsserviços — Visão Geral'"
else
    echo "Observabilidade: NAO iniciada (veja o aviso [0/4] acima)"
fi
echo "=================================================="

# Manter o processo vivo aguardando os serviços
wait
