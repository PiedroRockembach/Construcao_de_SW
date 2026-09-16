# Technical Specification: Sistema de Microserviços de Peças, Clientes e Representantes

## 1. Executive Summary
O sistema consiste em uma arquitetura orientada a microsserviços desenvolvida com o ecossistema **Spring Cloud** e **Spring Boot 3 (Java 17)**. A solução gerencia o ciclo de vida e operações de **Peças**, **Clientes** e **Representantes Comerciais**, garantindo alta coesão, baixo acoplamento, resiliência e facilidade de escala.

O ecossistema implementa rigorosamente os padrões solicitados:
- **Centralized Configuration**: Spring Cloud Config Server provendo parâmetros centralizados por perfil.
- **Service Discovery & Registry**: Spring Cloud Netflix Eureka Server para registro dinâmico e localização transparente dos nós de serviço.
- **API Gateway**: Spring Cloud Gateway como único ponto de entrada para clientes e frontend, com roteamento dinâmico baseado no Eureka, balanceamento de carga e configuração CORS.
- **Frontend / Client Testing**: Interface Web SPA intuitiva servida pelo próprio Gateway (ou acessível via navegador) e coleção Postman para testes integrados direcionados exclusivamente ao Gateway (`http://localhost:8080`).
- **Observabilidade (Métricas)** *(Ciclo 2)*: todos os módulos expõem métricas via **Micrometer + Spring Boot Actuator** no formato Prometheus; um container **Prometheus** coleta (scrape) essas métricas e um container **Grafana** as visualiza em dashboards provisionados automaticamente. Detalhes na **Seção 7**.

---

## 2. Requirements & Scope

### 2.1. Requisitos Funcionais

#### 2.1.1. Microserviço de Peças (`pecas-service`)
- **RF01 - Cadastrar Peça**: Permitir cadastrar uma peça informando:
  - Número de identificação da peça (`numeroIdentificacao` - String/Long, único)
  - Nome da peça (`nome` - String)
  - Descrição da peça (`descricao` - String)
- **RF02 - Consultar Peça por ID**: Buscar peça pelo seu identificador único.
- **RF03 - Consultar Peça por Nome**: Buscar peças por filtro de nome (busca exata ou parcial).
- **RF04 - Listar Peças**: Retornar todas as peças cadastradas.

#### 2.1.2. Microserviço de Clientes (`clientes-service`)
- **RF05 - Cadastrar Cliente**: Permitir cadastrar um cliente informando:
  - CPF do cliente (`cpf` - String, validado e único)
  - Nome do cliente (`nome` - String)
- **RF06 - Consultar Cliente por CPF**: Buscar cliente pelo CPF.
- **RF07 - Consultar Cliente por Nome**: Buscar clientes cujo nome contenha o termo pesquisado.
- **RF08 - Listar Clientes**: Retornar todos os clientes cadastrados.

#### 2.1.3. Microserviço de Representantes Comerciais (`representantes-service`)
- **RF09 - Cadastrar Representante**: Permitir cadastrar um representante informando:
  - CPF do representante (`cpf` - String, validado e único)
  - Nome do representante (`nome` - String)
- **RF10 - Consultar Representante por CPF**: Buscar representante pelo CPF.
- **RF11 - Consultar Representante por Nome**: Buscar representantes cujo nome contenha o termo pesquisado.
- **RF12 - Listar Representantes**: Retornar todos os representantes cadastrados.

#### 2.1.4. Infraestrutura Spring Cloud e Testabilidade
- **RF13 - Gateway Único**: O Gateway deve interceptar todo o tráfego externo e rotear dinamicamente para os serviços `pecas-service`, `clientes-service` e `representantes-service`.
- **RF14 - Service Discovery**: Todos os microsserviços de negócio e o Gateway devem se autorregistrar e resolver instâncias através do Eureka Server.
- **RF15 - Configuração Centralizada**: Config Server local nativo com repositório de configurações centralizadas de cada microsserviço.
- **RF16 - Frontend de Teste Integrado**: Uma interface interativa (HTML/CSS/JS moderno) que permita ao usuário cadastrar, listar e consultar Peças, Clientes e Representantes diretamente pelo Gateway.

---

### 2.2. Requisitos Não-Funcionais
- **Linguagem & Plataforma**: Java 17, Apache Maven.
- **Framework Principal**: Spring Boot 3.2.x / Spring Cloud 2023.0.x.
- **Persistência**: Spring Data JPA com banco de dados em memória H2 (isolado por microsserviço), com opção de console web em cada serviço.
- **Padrão Arquitetural**: RESTful APIs retornando JSON padronizado com códigos HTTP semânticos (200, 201, 400, 404, 500).
- **Isolamento de Dados**: Cada microsserviço possui sua própria base de dados e suas próprias entidades, preservando o princípio de Database per Service.

---

## 3. Architecture & Tech Stack

```
                                  +---------------------------------------+
                                  |     Frontend Web (SPA) / Postman     |
                                  +---------------------------------------+
                                                      |
                                                      | Requests (HTTP :8080)
                                                      v
                                  +---------------------------------------+
                                  |    Spring Cloud API Gateway (:8080)   |
                                  +---------------------------------------+
                                       /              |              \
           lb://pecas-service         /               |               \  lb://representantes-service
                                     /    lb://clientes-service        \
                                    v                 v                 v
                 +--------------------+    +--------------------+    +--------------------+
                 |   pecas-service    |    |  clientes-service  |    |representantes-serv |
                 |      (:8081)       |    |      (:8082)       |    |      (:8083)       |
                 |      (H2 DB)       |    |      (H2 DB)       |    |      (H2 DB)       |
                 +--------------------+    +--------------------+    +--------------------+
                           ^                          ^                         ^
                           |                          |                         |
               Heartbeats & Lookup        Heartbeats & Lookup       Heartbeats & Lookup
                           \                          |                         /
                            +-------------------------+------------------------+
                                                      |
                                                      v
                                  +---------------------------------------+
                                  |     Eureka Discovery Server (:8761)   |
                                  +---------------------------------------+
                                                      ^
                                                      |
                                  +---------------------------------------+
                                  |    Config Server (Native/FS) (:8888)  |
                                  +---------------------------------------+
```

### 3.1. Módulos do Sistema

| Módulo | Porta | Descrição |
| :--- | :--- | :--- |
| **`config-server`** | `8888` | Servidor central de propriedades (Spring Cloud Config) em modo nativo |
| **`discovery-server`** | `8761` | Servidor de registro e descoberta (Spring Cloud Netflix Eureka) |
| **`gateway-service`** | `8080` | Ponto de entrada unificado (Spring Cloud Gateway) + Interface Web UI estática |
| **`pecas-service`** | `8081` | Microsserviço de gerenciamento de peças (REST + Spring Data JPA + H2) |
| **`clientes-service`** | `8082` | Microsserviço de gerenciamento de clientes (REST + Spring Data JPA + H2) |
| **`representantes-service`**| `8083` | Microsserviço de gerenciamento de representantes comerciais (REST + Spring Data JPA + H2) |

---

## 4. API Endpoints Specification (Expostos via Gateway em `http://localhost:8080`)

### 4.1. Peças (`/api/pecas/**`)
- `POST /api/pecas`
  - Body: `{"numeroIdentificacao": "P-001", "nome": "Engrenagem Cônica", "descricao": "Aço temperado 1045"}`
  - Status: `201 Created`
- `GET /api/pecas`
  - Status: `200 OK` (Retorna lista com todas as peças)
- `GET /api/pecas/{id}`
  - Status: `200 OK` ou `404 Not Found`
- `GET /api/pecas/busca?nome={nome}`
  - Status: `200 OK` (Retorna peças com nome correspondente)

### 4.2. Clientes (`/api/clientes/**`)
- `POST /api/clientes`
  - Body: `{"cpf": "123.456.789-00", "nome": "Carlos Silva"}`
  - Status: `201 Created`
- `GET /api/clientes`
  - Status: `200 OK` (Retorna lista com todos os clientes)
- `GET /api/clientes/cpf/{cpf}`
  - Status: `200 OK` ou `404 Not Found`
- `GET /api/clientes/busca?nome={nome}`
  - Status: `200 OK` (Retorna clientes cujo nome contém o termo)

### 4.3. Representantes Comerciais (`/api/representantes/**`)
- `POST /api/representantes`
  - Body: `{"cpf": "987.654.321-11", "nome": "Ana Beatriz"}`
  - Status: `201 Created`
- `GET /api/representantes`
  - Status: `200 OK` (Retorna lista com todos os representantes)
- `GET /api/representantes/cpf/{cpf}`
  - Status: `200 OK` ou `404 Not Found`
- `GET /api/representantes/busca?nome={nome}`
  - Status: `200 OK` (Retorna representantes com nome correspondente)

---

## 5. State Management & Data Flow
1. **Inicialização**:
   - `config-server` sobe primeiro na porta `8888`, servindo arquivos YAML/properties da pasta central de configuração.
   - `discovery-server` (Eureka) inicia na porta `8761`.
   - `pecas-service`, `clientes-service` e `representantes-service` consultam o `config-server`, obtêm suas configurações e registram-se no `discovery-server`.
   - `gateway-service` inicializa, carrega suas rotas dinâmicas do Eureka e serve a aplicação frontend em `/`.
2. **Ciclo de Requisição**:
   - O usuário interage com a UI ou executa chamadas HTTP via Postman apontando exclusivamente para `http://localhost:8080/api/...`.
   - O Gateway consulta a rota correspondente via Service Discovery (`lb://pecas-service`, etc.) e despacha a requisição para a instância saudável.
   - A resposta transita de volta pelo Gateway com os devidos cabeçalhos de resposta HTTP e JSON.

---

## 6. Project Layout in `app_build/`
```
app_build/
├── pom.xml                               # Parent POM multi-módulo (Spring Boot 3.2.x, Cloud 2023.0.x)
├── config-server/                        # Módulo Config Server
│   ├── pom.xml
│   └── src/main/...
├── config-repo/                          # Diretório de arquivos de configuração centralizados (.properties / .yml)
│   ├── application.yml
│   ├── pecas-service.yml
│   ├── clientes-service.yml
│   ├── representantes-service.yml
│   └── gateway-service.yml
├── discovery-server/                     # Módulo Eureka Server
│   ├── pom.xml
│   └── src/main/...
├── gateway-service/                      # Módulo Spring Cloud Gateway
│   ├── pom.xml
│   └── src/main/resources/static/        # Frontend interativo (Dashboard SPA)
├── pecas-service/                        # Microsserviço de Peças
│   ├── pom.xml
│   └── src/main/...
├── clientes-service/                     # Microsserviço de Clientes
│   ├── pom.xml
│   └── src/main/...
├── representantes-service/               # Microsserviço de Representantes
│   ├── pom.xml
│   └── src/main/...
└── postman_collection.json               # Coleção de testes para Postman/Insomnia
```


---

## 7. Observabilidade: Métricas com Prometheus & Grafana (Ciclo 2)

> Referência: `observabilidade.html` (Pilar 2 — Métricas: Micrometer + Actuator + Prometheus + Grafana).
> **Escopo deste ciclo**: somente o pilar de **métricas**. Logs estruturados (JSON/Logstash) e rastreamento distribuído (OpenTelemetry/Jaeger) ficam **fora de escopo** e são candidatos a ciclos futuros.

### 7.1. Requisitos Funcionais

- **RF17 - Exposição de Métricas**: Todos os 6 módulos (`config-server`, `discovery-server`, `gateway-service`, `pecas-service`, `clientes-service`, `representantes-service`) devem expor o endpoint `GET /actuator/prometheus` no formato de texto do Prometheus.
- **RF18 - Métricas Padrão**: Cada módulo deve publicar as métricas automáticas do Micrometer/Actuator:
  - HTTP: `http_server_requests_seconds` (contagem, soma, máximo e **buckets de histograma** para cálculo de percentis p50/p95/p99), com tags `uri`, `method`, `status`, `outcome`.
  - JVM: memória (`jvm_memory_used_bytes`), GC (`jvm_gc_pause_seconds`), threads (`jvm_threads_live_threads`), classes.
  - Sistema/Processo: `process_cpu_usage`, `system_cpu_usage`, `process_uptime_seconds`.
  - Pool de conexões (serviços com JPA): `hikaricp_connections_*`.
  - Gateway: métricas de rotas do Spring Cloud Gateway (`spring_cloud_gateway_requests_seconds`) com tags `routeId`, `httpStatusCode`.
- **RF19 - Tag Comum de Aplicação**: Toda métrica deve conter a tag `application=${spring.application.name}`, permitindo filtrar/agrupar por serviço no Grafana.
- **RF20 - Métricas de Negócio Customizadas** (Micrometer `Counter`/`Gauge`, conforme exemplo `users.created.total` da referência):

  | Serviço | Métrica (nome Micrometer → nome Prometheus) | Tipo | Tags | Descrição |
  | :--- | :--- | :--- | :--- | :--- |
  | `pecas-service` | `pecas.cadastro` → `pecas_cadastro_total` | Counter | `resultado=sucesso\|conflito` | Tentativas de cadastro de peças |
  | `pecas-service` | `pecas.consulta.nao_encontrada` → `pecas_consulta_nao_encontrada_total` | Counter | — | Consultas por ID/número que retornaram 404 |
  | `pecas-service` | `pecas.registros` → `pecas_registros` | Gauge | — | Quantidade atual de peças na base |
  | `clientes-service` | `clientes.cadastro` → `clientes_cadastro_total` | Counter | `resultado=sucesso\|conflito` | Tentativas de cadastro de clientes |
  | `clientes-service` | `clientes.consulta.nao_encontrada` → `clientes_consulta_nao_encontrada_total` | Counter | — | Consultas por CPF que retornaram 404 |
  | `clientes-service` | `clientes.registros` → `clientes_registros` | Gauge | — | Quantidade atual de clientes na base |
  | `representantes-service` | `representantes.cadastro` → `representantes_cadastro_total` | Counter | `resultado=sucesso\|conflito` | Tentativas de cadastro de representantes |
  | `representantes-service` | `representantes.consulta.nao_encontrada` → `representantes_consulta_nao_encontrada_total` | Counter | — | Consultas por CPF que retornaram 404 |
  | `representantes-service` | `representantes.registros` → `representantes_registros` | Gauge | — | Quantidade atual de representantes na base |

  A instrumentação ocorre na camada **Service** (ex.: `PecaService`), injetando `MeterRegistry` via construtor — sem alterar contratos REST existentes.

- **RF21 - Container Prometheus**: Um container Prometheus deve coletar as métricas de todos os módulos a cada **5s** e ficar acessível em `http://localhost:9090`, com retenção local de dados em volume Docker.
- **RF22 - Container Grafana**: Um container Grafana deve ficar acessível em `http://localhost:3000`, com:
  - **Datasource Prometheus provisionado automaticamente** (sem configuração manual pela UI).
  - **Dashboard "Microsserviços — Visão Geral" provisionado automaticamente**, contendo os painéis:
    1. Status (UP/DOWN) de cada serviço (`up`).
    2. Taxa de requisições por serviço (req/s).
    3. Latência p95 por serviço (histograma).
    4. Taxa de erros HTTP 4xx/5xx por serviço.
    5. Requisições por rota no Gateway.
    6. Uso de memória heap da JVM por serviço.
    7. Uso de CPU do processo por serviço.
    8. Cadastros de negócio (sucesso × conflito) e quantidade de registros por domínio.
  - Variável de dashboard `application` (multi-seleção) para filtrar serviços.
- **RF23 - Orquestração**: Um único `docker compose up -d` (em `app_build/observability/`) sobe Prometheus + Grafana. Os scripts `start-all.sh`/`stop-all.sh` passam a subir/derrubar a stack de observabilidade quando o Docker estiver disponível (sem falhar caso não esteja).

### 7.2. Requisitos Não-Funcionais

- **Baixo impacto**: nenhuma alteração nos endpoints de negócio, portas ou fluxo de roteamento existentes.
- **Versões fixadas** das imagens Docker (sem `latest`) para reprodutibilidade: `prom/prometheus:v2.53.x` e `grafana/grafana:11.x`.
- **Microsserviços continuam rodando no host** (via `start-all.sh`/`java -jar`); os containers alcançam o host por `host.docker.internal` (mapeado com `extra_hosts: host-gateway`, compatível com Linux).
- **Segurança**: credenciais do Grafana configuráveis via variáveis de ambiente (`GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD`, padrão `admin`/`admin` apenas para ambiente acadêmico/local); cadastro de novos usuários desabilitado.
- **Configuração centralizada preservada**: as propriedades de métricas comuns ficam em `config-repo/application.yml` (servidas pelo Config Server), com fallback equivalente no `application.yml` local de cada módulo, para que as métricas funcionem mesmo se o Config Server estiver indisponível.

### 7.3. Arquitetura de Observabilidade

```
   Host (java -jar)                                               Docker (compose)
 +------------------------------------------+          +-------------------------------------+
 | config-server       :8888 /actuator/prom |<--+      |                                     |
 | discovery-server    :8761 /actuator/prom |<--+      |   +-----------------------------+   |
 | gateway-service     :8080 /actuator/prom |<--+------+---|  Prometheus :9090           |   |
 | pecas-service       :8081 /actuator/prom |<--+ scrape   |  (scrape 5s, TSDB em volume)|   |
 | clientes-service    :8082 /actuator/prom |<--+ via      +--------------+--------------+   |
 | representantes-svc  :8083 /actuator/prom |<--+ host.docker.internal    | PromQL          |
 +------------------------------------------+          |                  v                  |
                                                       |   +-----------------------------+   |
                  Usuário (navegador) ---------------->+---|  Grafana :3000              |   |
                                                       |   |  datasource + dashboard     |   |
                                                       |   |  provisionados              |   |
                                                       |   +-----------------------------+   |
                                                       +-------------------------------------+
```

**Decisão — descoberta de alvos**: uso de **`static_configs`** no Prometheus (portas fixas conhecidas) em vez de `eureka_sd_configs`. Justificativa: as portas são fixas por especificação, o Config Server e o Eureka não se auto-registram, e a configuração estática é mais simples e previsível em ambiente local. Cada alvo recebe o label `application` equivalente ao nome do serviço.

### 7.4. Stack & Alterações por Módulo

| Item | Alteração |
| :--- | :--- |
| `pom.xml` dos 6 módulos | + `io.micrometer:micrometer-registry-prometheus` (versão gerida pelo Spring Boot BOM); `spring-boot-starter-actuator` presente em todos |
| `config-repo/application.yml` (+ cópia em `config-server/src/main/resources/config-repo/`) | exposição `health,info,refresh,prometheus,metrics`; `management.metrics.tags.application`; `management.metrics.distribution.percentiles-histogram.http.server.requests=true` |
| `application.yml` locais dos 6 módulos | mesmas propriedades de métricas (fallback) |
| `*Service.java` dos 3 serviços de negócio | Counters e Gauge descritos no RF20 |
| `app_build/observability/` | **novo** — `docker-compose.yml`, `prometheus/prometheus.yml`, `grafana/provisioning/datasources/prometheus.yml`, `grafana/provisioning/dashboards/dashboards.yml`, `grafana/dashboards/microsservicos-overview.json` |
| `start-all.sh` / `stop-all.sh` | sobem/derrubam a stack de observabilidade (se Docker disponível) e exibem as URLs |
| `README.md` e UI do Gateway | seção de Observabilidade com links para Prometheus (`:9090`) e Grafana (`:3000`) e exemplos de PromQL |

**Portas adicionadas:**

| Componente | Porta | Descrição |
| :--- | :--- | :--- |
| **Prometheus** (container) | `9090` | Coleta e armazenamento de séries temporais; UI de consultas PromQL |
| **Grafana** (container) | `3000` | Dashboards de visualização |

### 7.5. Fluxo de Dados das Métricas

1. Cada requisição HTTP e evento de negócio atualiza medidores no `MeterRegistry` (em memória) do respectivo serviço.
2. A cada 5s o Prometheus executa `GET http://host.docker.internal:<porta>/actuator/prometheus` em cada alvo, armazenando as amostras em sua TSDB (volume `prometheus-data`).
3. O Grafana consulta o Prometheus via PromQL (ex.: `histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[1m])))`) e renderiza os painéis.
4. Métricas são efêmeras nos serviços (reiniciar um serviço zera os counters); o Prometheus preserva o histórico e `rate()`/`increase()` tratam os resets.

### 7.6. Critérios de Aceite

- [ ] `curl http://localhost:808{0..3}/actuator/prometheus`, `:8761` e `:8888` retornam métricas com a tag `application`.
- [ ] Em `http://localhost:9090/targets`, os 6 alvos aparecem como **UP**.
- [ ] Após `POST /api/pecas` via Gateway, a consulta `pecas_cadastro_total{resultado="sucesso"}` incrementa no Prometheus.
- [ ] Em `http://localhost:3000`, o dashboard "Microsserviços — Visão Geral" abre já com dados, sem configuração manual.
