# Sistema de Microserviços Spring Cloud

Este projeto implementa uma arquitetura completa de microsserviços para gestão de **Peças**, **Clientes** e **Representantes Comerciais**, utilizando os padrões **API Gateway**, **Service Discovery** (Eureka) e **Configuração Centralizada** (Spring Cloud Config), com banco de dados em memória **H2** e interface de usuário integrada.

---

## 🏛️ Arquitetura do Sistema

- **Config Server** (`porta 8888`): Servidor centralizado de configurações por ambiente.
- **Discovery Server** (`porta 8761`): Registro e descoberta dinâmica com Netflix Eureka.
- **API Gateway** (`porta 8080`): Ponto único de entrada, roteamento dinâmico e anfitrião do Frontend Web.
- **`pecas-service`** (`porta 8081`): Microsserviço de gestão de peças (`numeroIdentificacao`, `nome`, `descricao`).
- **`clientes-service`** (`porta 8082`): Microsserviço de gestão de clientes (`cpf`, `nome`).
- **`representantes-service`** (`porta 8083`): Microsserviço de gestão de representantes comerciais (`cpf`, `nome`).

---

## 🚀 Ponto de Acesso & Interface Web (Apenas Gateway)

Acesse diretamente no navegador o painel completo:
👉 **[http://localhost:8080](http://localhost:8080)**

Neste painel você pode:
- Cadastrar novas Peças, Clientes e Representantes;
- Consultar registros por ID, por CPF ou por filtro parcial de Nome;
- Listar todos os itens cadastrados em tempo real;
- Clicar no botão **"Popular Dados Demo"** para carregar rapidamente dados de teste em todos os microsserviços.

---

## 📬 Coleção Postman / Insomnia

O arquivo de coleção pronto para importação encontra-se em:
📂 `app_build/postman_collection.json`

Todas as requisições apontam estritamente para o **API Gateway**:
- **Peças**:
  - `POST http://localhost:8080/api/pecas`
  - `GET  http://localhost:8080/api/pecas`
  - `GET  http://localhost:8080/api/pecas/{id}`
  - `GET  http://localhost:8080/api/pecas/busca?nome={termo}`
- **Clientes**:
  - `POST http://localhost:8080/api/clientes`
  - `GET  http://localhost:8080/api/clientes`
  - `GET  http://localhost:8080/api/clientes/cpf/{cpf}`
  - `GET  http://localhost:8080/api/clientes/busca?nome={termo}`
- **Representantes**:
  - `POST http://localhost:8080/api/representantes`
  - `GET  http://localhost:8080/api/representantes`
  - `GET  http://localhost:8080/api/representantes/cpf/{cpf}`
  - `GET  http://localhost:8080/api/representantes/busca?nome={termo}`

---

## 🛠️ Comandos de Orquestração

- **Iniciar todos os serviços**:
  ```bash
  cd app_build
  ./start-all.sh
  ```

- **Parar todos os serviços**:
  ```bash
  cd app_build
  ./stop-all.sh
  ```

- **Eureka Dashboard**:
  [http://localhost:8761](http://localhost:8761)

---

## 📊 Observabilidade (Métricas: Micrometer + Prometheus + Grafana)

Todos os módulos expõem métricas em `/actuator/prometheus` (tag comum `application`). O `start-all.sh` sobe automaticamente
(se o Docker estiver disponível) os containers de **Prometheus** e **Grafana** definidos em `observability/docker-compose.yml`.

| Ferramenta | URL | Observação |
| :--- | :--- | :--- |
| Prometheus | [http://localhost:9090](http://localhost:9090) | Veja os alvos em `/targets` (6 serviços devem estar **UP**) |
| Grafana | [http://localhost:3000](http://localhost:3000) | Login `admin`/`admin` — dashboard **Microsserviços — Visão Geral** |

Subir/derrubar apenas a stack de observabilidade:
```bash
cd app_build/observability
docker compose up -d     # sobe
docker compose down      # derruba (use -v para apagar os dados)
```

Credenciais do Grafana podem ser alteradas via variáveis `GRAFANA_ADMIN_USER` e `GRAFANA_ADMIN_PASSWORD`.

**Métricas de negócio customizadas:**

| Métrica Prometheus | Tipo | Descrição |
| :--- | :--- | :--- |
| `pecas_cadastro_total{resultado}` / `clientes_cadastro_total{resultado}` / `representantes_cadastro_total{resultado}` | Counter | Cadastros por resultado (`sucesso` / `conflito`) |
| `pecas_consulta_nao_encontrada_total` (idem clientes/representantes) | Counter | Consultas que retornaram 404 |
| `pecas_registros` / `clientes_registros` / `representantes_registros` | Gauge | Quantidade atual de registros na base |

**Exemplos de PromQL:**
```promql
# Requisições por segundo por serviço
sum by (application) (rate(http_server_requests_seconds_count[1m]))

# Latência p95 por serviço
histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[1m])))

# Peças cadastradas com sucesso
pecas_cadastro_total{resultado="sucesso"}
```
