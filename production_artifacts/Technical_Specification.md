# Technical Specification: Observabilidade e Monitoramento com Prometheus e Grafana

## 1. Executive Summary
Esta especificação técnica detalha a expansão da arquitetura do ecossistema de microsserviços Spring Boot/Spring Cloud para incorporar **Observabilidade de Métricas** de ponta a ponta. 

O sistema atual (composto por `Config Server`, `Discovery Server`, `API Gateway`, `pecas-service`, `clientes-service` e `representantes-service`) será instrumentado com **Spring Boot Actuator** e **Micrometer Prometheus Registry** para expor métricas padronizadas da JVM, métricas de tráfego HTTP e métricas customizadas de negócio.

Para ingestão, armazenamento e visualização dessas métricas, a infraestrutura será complementada com:
1. **Container Prometheus**: Responsável pela coleta contínua (*pull scraping*) das métricas expostas pelos microsserviços através do endpoint `/actuator/prometheus`.
2. **Container Grafana**: Responsável pela visualização rica e painéis em tempo real (*dashboards*), com provisionamento automatizado da fonte de dados (Datasource) e dashboards pré-carregados para visualização do status dos serviços e métricas JVM/HTTP.

---

## 2. Requirements & Scope

### 2.1. Requisitos Funcionais

#### 2.1.1. Instrumentação das Aplicações (Microsserviços)
- **RF-MET-01 - Exposição do Endpoint Prometheus**:
  - Todos os serviços de negócio (`pecas-service`, `clientes-service`, `representantes-service`) e o `gateway-service` devem expor o endpoint `/actuator/prometheus`.
  - A dependência `micrometer-registry-prometheus` deve ser adicionada no `pom.xml` raiz (`microservicos-parent`) e nos `pom.xml` dos serviços.
- **RF-MET-02 - Configuração Centralizada de Exposição de Métricas**:
  - O `config-repo/application.yml` deve expor os endpoints `health,info,refresh,prometheus,metrics` para que todos os serviços herdem a configuração automaticamente.
  - Habilitar tags comuns para identificação dos serviços nas métricas (ex.: `application=${spring.application.name}`).
- **RF-MET-03 - Métricas de Negócio Customizadas**:
  - `pecas-service`: Contador Micrometer para registrar total de peças cadastradas (`pecas.created.total`).
  - `clientes-service`: Contador Micrometer para registrar total de clientes cadastrados (`clientes.created.total`).
  - `representantes-service`: Contador Micrometer para registrar total de representantes cadastrados (`representantes.created.total`).

#### 2.1.2. Infraestrutura Prometheus
- **RF-MET-04 - Container Prometheus**:
  - Configurado via `docker-compose.yml` utilizando imagem oficial `prom/prometheus:v2.51.2` (ou `latest`).
  - Porta exposta: `9090`.
  - Volume mapeado para arquivo de configuração `prometheus.yml`.
- **RF-MET-05 - Scrape Jobs**:
  - Scrape Jobs configurados no `prometheus.yml` para coletar métricas a cada 5 segundos de:
    - `gateway-service` (porta 8080 / path `/actuator/prometheus`)
    - `pecas-service` (porta 8081 / path `/actuator/prometheus`)
    - `clientes-service` (porta 8082 / path `/actuator/prometheus`)
    - `representantes-service` (porta 8083 / path `/actuator/prometheus`)
  - Suporte à comunicação entre container e host local via `host.docker.internal` (com `extra_hosts: ["host.docker.internal:host-gateway"]`).

#### 2.1.3. Infraestrutura Grafana
- **RF-MET-06 - Container Grafana**:
  - Configurado via `docker-compose.yml` utilizando imagem oficial `grafana/grafana:10.4.2` (ou `latest`).
  - Porta exposta: `3000`.
  - Credenciais padrão definidas por variáveis de ambiente (usuário: `admin`, senha: `admin`).
- **RF-MET-07 - Provisionamento Automatizado de Datasource**:
  - Arquivo de configuração `provisioning/datasources/datasource.yml` configurado para apontar para o Prometheus (`http://prometheus:9090` ou URL de rede interna Docker) sem necessidade de setup manual pelo usuário.
- **RF-MET-08 - Provisionamento Automatizado de Dashboard**:
  - Dashboard(s) pré-configurado(s) em JSON na pasta `provisioning/dashboards/` contendo:
    - Visão geral de saúde dos serviços (*Up / Down*).
    - Gráfico de requisições por segundo (taxa de RPS por rota/serviço).
    - Latência média / tempo de resposta HTTP (`http_server_requests_seconds_count` e `_sum`).
    - Uso de memória Heap da JVM (`jvm_memory_used_bytes`).
    - Métricas customizadas de negócio (peças, clientes, representantes cadastrados).

---

### 2.2. Requisitos Não-Funcionais
- **RNF-01 - Desacoplamento e Baixo Impacto**: A coleta de métricas via Micrometer opera em memória com impacto desprezível na latência das operações da API.
- **RNF-02 - Prontidão para Execução**: Toda a stack de observabilidade deve subir com um único comando (`docker compose up -d` ou através de script integrado).
- **RNF-03 - Portabilidade Multi-plataforma**: A resolução de rede entre containers e os serviços Spring Boot rodando no host deve funcionar consistentemente em Linux, macOS e Windows.

---

## 3. Architecture & Tech Stack

```
   +-------------------------------------------------------------------------------+
   |                             BROWSER DO USUÁRIO                                |
   |  - API Gateway / Frontend: http://localhost:8080                              |
   |  - Prometheus UI:          http://localhost:9090                              |
   |  - Grafana Dashboards:     http://localhost:3000 (admin/admin)                |
   +-------------------------------------------------------------------------------+
                  |                               |                      |
                  | HTTP                          | HTTP (:9090)         | HTTP (:3000)
                  v                               v                      v
     +-------------------------+      +---------------------+   +---------------------+
     |   API Gateway (:8080)   |      |  PROMETHEUS (Docker)|<--|   GRAFANA (Docker)  |
     |   /actuator/prometheus  |      |   porta 9090        |   |   porta 3000        |
     +-------------------------+      |                     |   | Datasource Automático|
                  |                   | Scrapes periódicos  |   | Dashboards Prontos  |
                  | (Eureka Routing)  | a cada 5s           |   +---------------------+
                  v                   +---------------------+
      +------------------------+                 |
      | pecas-service (:8081)  |<----------------+
      | /actuator/prometheus   |                 |
      +------------------------+                 |
      | clientes-serv. (:8082) |<----------------+
      | /actuator/prometheus   |                 |
      +------------------------+                 |
      | repres.-serv.  (:8083) |<----------------+
      | /actuator/prometheus   |
      +------------------------+
```

### 3.1. Tecnologias Empregadas

| Componente | Tecnologia / Versão | Função Principal |
| :--- | :--- | :--- |
| **Parent & Framework** | Spring Boot 3.2.5 / Java 17 | Base dos microsserviços existentes |
| **Métricas Core** | Spring Boot Actuator 3.2.5 | Exposição de endpoints operacionais |
| **Micrometer Adapter** | `micrometer-registry-prometheus` | Serialização das métricas para formato padrão Prometheus |
| **TSDB & Monitor** | Prometheus `v2.51.2` | Coleta por scraping HTTP e armazenamento temporal de métricas |
| **Visualizador / UI** | Grafana `10.4.2` | Dashboards dinâmicos com gráficos de infraestrutura e negócio |
| **Container Engine** | Docker & Docker Compose v2 | Orquestração local dos containers de Prometheus e Grafana |

---

## 4. Estrutura de Arquivos e Alterações

### 4.1. Modificações no Código Spring Boot
1. **`app_build/pom.xml`**: Adição de `io.micrometer:micrometer-registry-prometheus` no `<dependencyManagement>` ou nas dependências globais dos serviços.
2. **`app_build/config-repo/application.yml`**:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,refresh,prometheus,metrics
     endpoint:
       prometheus:
         enabled: true
     metrics:
       tags:
         application: ${spring.application.name}
   ```
3. **`app_build/pecas-service`**, **`clientes-service`**, **`representantes-service`**:
   - Inclusão da dependência `micrometer-registry-prometheus`.
   - Adição do `MeterRegistry` nos respectivos Services para registrar contadores de eventos de criação (ex: `pecas.created.total`, `clientes.created.total`, `representantes.created.total`).
4. **`app_build/gateway-service`**:
   - Inclusão de `micrometer-registry-prometheus` para coletar métricas de roteamento e volume de requisições de entrada.

### 4.2. Novos Arquivos de Infraestrutura (Observabilidade)
Diretório: `observabilidade/` (ou `docker/observabilidade/`):
- `docker-compose.yml`: Definição dos serviços `prometheus` e `grafana`, redes, volumes e portas.
- `prometheus/prometheus.yml`: Configuração dos jobs de scraping para os 4 serviços Spring.
- `grafana/provisioning/datasources/datasource.yml`: Datasource automático apontando para o Prometheus.
- `grafana/provisioning/dashboards/dashboards.yml`: Provedor automático de dashboards.
- `grafana/dashboards/microservices-dashboard.json`: Dashboard completo com visualização de JVM, status dos serviços e contadores de negócio.
- Scripts de automação:
  - `start-monitoring.sh` / `stop-monitoring.sh` para facilitar o ciclo de vida dos containers.

---

## 5. Fluxo de Dados e Estado (State Management)

1. **Geração de Métricas**:
   - A cada requisição atendida (ex: `POST /api/pecas`), o Spring Boot Actuator / Micrometer atualiza os histogramas `http_server_requests_seconds` e incrementa o contador `pecas_created_total`.
2. **Exportação**:
   - O endpoint HTTP `GET /actuator/prometheus` compila o estado corrente de todos os medidores registrados e o formata segundo a sintaxe OpenMetrics/Prometheus.
3. **Ingestão no Prometheus**:
   - O Prometheus dispara requisições HTTP GET para os nós cadastrados em intervalos configurados (5s). Os dados são persistidos na Time Series Database interna do Prometheus.
4. **Consulta e Renderização no Grafana**:
   - O Grafana consulta o Prometheus via PromQL (ex: `rate(http_server_requests_seconds_count[1m])`, `jvm_memory_used_bytes`, `pecas_created_total`), gerando gráficos interativos e painéis de status em tempo real.
