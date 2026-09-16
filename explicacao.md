# Ciclo 2 — Observabilidade (Métricas): o que foi feito, por quê e como usar

Este documento descreve **tudo o que foi adicionado/alterado no projeto** durante a sessão de trabalho
dedicada à observabilidade, os **motivos** de cada decisão e a **forma de uso** no dia a dia.

O pedido que originou o trabalho está em [prompt.md](prompt.md):

> configure/ajuste o projeto para inserir aspectos de observabilidade: reportar métricas através do
> **Prometheus** e visualizá-las através do **Grafana**; configurar um container para o Prometheus e um
> para o Grafana; utilizar o arquivo `observabilidade.html` como referência.

O arquivo [observabilidade.html](observabilidade.html) (material de referência da disciplina) descreve os
três pilares da observabilidade — **logs**, **métricas** e **traces**. Neste ciclo foi implementado
**apenas o pilar de métricas** (Micrometer + Actuator + Prometheus + Grafana). Logs estruturados
(JSON/Logstash) e rastreamento distribuído (OpenTelemetry/Jaeger) ficaram **fora de escopo**,
registrados como candidatos a ciclos futuros.

---

## 1. Visão geral do que mudou

| Categoria | O que foi feito |
| :--- | :--- |
| **Instrumentação** | Os 6 módulos passaram a expor `/actuator/prometheus` via Micrometer |
| **Métricas de negócio** | Counters e Gauges customizados nos 3 serviços de domínio |
| **Infraestrutura** | Nova pasta `app_build/observability/` com Prometheus + Grafana em Docker Compose |
| **Automação** | `start-all.sh` / `stop-all.sh` sobem e derrubam a stack junto com a aplicação |
| **Documentação** | Nova Seção 7 na `Technical_Specification.md` e nova seção no `README.md` |
| **UI** | Botões de atalho para Prometheus e Grafana no cabeçalho do frontend do Gateway |

Nenhum endpoint de negócio, porta de serviço ou rota do Gateway foi alterado — o ciclo é **aditivo**.

---

## 2. Instrumentação dos microsserviços

### 2.1. Dependência Micrometer (6 `pom.xml`)

Foi adicionada em `config-server`, `discovery-server`, `gateway-service`, `pecas-service`,
`clientes-service` e `representantes-service`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Motivo:** o `spring-boot-starter-actuator` já estava presente em todos os módulos, mas sozinho ele não
sabe *serializar* as métricas no formato de texto do Prometheus. O registry do Micrometer é o que cria o
endpoint `/actuator/prometheus`. A versão é gerida pelo BOM do Spring Boot (sem `<version>`), evitando
divergência entre módulos.

### 2.2. Configuração de métricas (YAMLs)

Adicionado em `config-repo/application.yml` (e na cópia em
`config-server/src/main/resources/config-repo/application.yml`), com **fallback equivalente** no
`application.yml` local de cada módulo:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh,prometheus,metrics   # no config-repo
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
  prometheus:
    metrics:
      export:
        enabled: true
```

**Motivos:**
- `tags.application` — adiciona a tag `application` a **toda** métrica, permitindo agrupar/filtrar por
  serviço no Grafana (`sum by (application) (...)`).
- `percentiles-histogram` — publica os *buckets* de histograma de `http_server_requests_seconds`, sem os
  quais não é possível calcular p95/p99 com `histogram_quantile()` no PromQL.
- **Duplicação intencional** (config-repo + local): a configuração centralizada é a fonte da verdade, mas
  o fallback local garante que as métricas continuem funcionando mesmo se o Config Server estiver fora.

### 2.3. Métricas de negócio customizadas

Em `PecaService`, `ClienteService` e `RepresentanteService` o `MeterRegistry` passou a ser injetado por
construtor e três medidores foram registrados por serviço:

| Métrica (Prometheus) | Tipo | Significado |
| :--- | :--- | :--- |
| `pecas_cadastro_total{resultado="sucesso\|conflito"}` | Counter | Cadastros bem-sucedidos vs. rejeitados por duplicidade |
| `pecas_consulta_nao_encontrada_total` | Counter | Consultas que resultaram em 404 |
| `pecas_registros` | Gauge | Quantidade atual de registros na base (`repository.count()`) |

(o mesmo para `clientes_*` e `representantes_*`).

**Motivos:**
- A instrumentação fica na camada **Service**, não no Controller — é ali que a regra de negócio decide
  entre sucesso e conflito, e assim os contratos REST não mudam.
- O *Counter* de conflito é incrementado antes de lançar a `ResponseStatusException`; nos métodos de
  busca, o incremento foi movido para dentro do `orElseThrow(() -> { ... })` justamente para só contar
  quando o 404 realmente acontece.
- O *Gauge* usa uma função sobre o repositório, então é avaliado no momento do scrape (valor sempre atual).
- Essas métricas respondem perguntas que as métricas técnicas não respondem: "quantas peças estão sendo
  cadastradas?", "há muitas tentativas de cadastro duplicado?".

---

## 3. Stack de observabilidade em containers

Nova pasta [app_build/observability/](app_build/observability/):

```
observability/
├── docker-compose.yml
├── prometheus/
│   └── prometheus.yml
└── grafana/
    ├── provisioning/
    │   ├── datasources/prometheus.yml
    │   └── dashboards/dashboards.yml
    └── dashboards/
        └── microsservicos-overview.json
```

### 3.1. `docker-compose.yml`

Dois serviços: **Prometheus** (`prom/prometheus:v2.53.2`, porta `9090`) e **Grafana**
(`grafana/grafana:11.2.0`, porta `3000`), com volumes nomeados `prometheus-data` e `grafana-data`.

**Motivos das escolhas:**
- **Versões fixadas** (sem `latest`) para reprodutibilidade — o projeto precisa subir igual na máquina do
  professor e na do aluno.
- **`extra_hosts: host.docker.internal:host-gateway`** — os microsserviços continuam rodando **no host**
  (`java -jar`, via `start-all.sh`); esse mapeamento é o que permite ao container alcançar o host
  também no **Linux**, onde `host.docker.internal` não existe por padrão.
- **Retenção de 7 dias** (`--storage.tsdb.retention.time=7d`) — suficiente para uma demonstração
  acadêmica sem encher o disco.
- **Credenciais parametrizáveis** por `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` (padrão
  `admin`/`admin`, aceitável apenas em ambiente local) e cadastro de novos usuários desabilitado.

### 3.2. `prometheus/prometheus.yml`

Scrape a cada **5s** em `/actuator/prometheus` dos 6 alvos, via `static_configs`.

**Motivo de `static_configs` em vez de `eureka_sd_configs`:** as portas são fixas por especificação e nem
o Config Server nem o próprio Eureka se registram no Eureka — a descoberta estática cobre 100% dos alvos,
é mais simples e mais previsível. Cada alvo recebe o label `application` com o nome do serviço, e
`honor_labels: true` evita que o Prometheus renomeie a tag vinda do Micrometer para `exported_application`.

### 3.3. Provisionamento do Grafana

- `provisioning/datasources/prometheus.yml` — datasource Prometheus (`http://prometheus:9090`, uid fixo
  `prometheus`) criado automaticamente na subida.
- `provisioning/dashboards/dashboards.yml` — carrega os dashboards da pasta montada.
- `dashboards/microsservicos-overview.json` — dashboard **"Microsserviços — Visão Geral"**, definido
  como home dashboard, com variável `application` (multi-seleção) e os painéis:

  | Linha | Painéis |
  | :--- | :--- |
  | Saúde dos Serviços | Status UP/DOWN por serviço |
  | Tráfego HTTP | Requisições/s, latência p95, taxa de erros 4xx/5xx, requisições por rota no Gateway |
  | JVM & Recursos | Memória heap utilizada, uso de CPU do processo |
  | Métricas de Negócio | Cadastros (sucesso × conflito), registros na base, consultas 404 |

**Motivo:** provisionamento por arquivo significa **zero configuração manual** — quem clonar o repositório
sobe os containers e já encontra datasource e dashboard prontos, o que também torna a demonstração
repetível e versionada no Git.

---

## 4. Automação e documentação

- **[start-all.sh](app_build/start-all.sh)** — novo passo `[0/4]` que executa
  `docker compose -f observability/docker-compose.yml up -d` **se** o Docker/Compose estiver disponível.
  Se não estiver, apenas avisa e segue: a aplicação nunca deixa de subir por causa da observabilidade.
  Ao final, o script imprime também as URLs do Prometheus e do Grafana.
- **[stop-all.sh](app_build/stop-all.sh)** — derruba a stack com `docker compose down`, **preservando os
  volumes** (o histórico de métricas sobrevive a um restart; use `down -v` para apagar de fato).
- **[app_build/README.md](app_build/README.md)** — nova seção de Observabilidade com URLs, tabela de
  métricas customizadas e exemplos de PromQL.
- **[production_artifacts/Technical_Specification.md](production_artifacts/Technical_Specification.md)** —
  nova **Seção 7** com requisitos RF17–RF23, requisitos não-funcionais, diagrama da arquitetura de
  observabilidade, decisões de projeto, fluxo dos dados e critérios de aceite.
- **[index.html do Gateway](app_build/gateway-service/src/main/resources/static/index.html)** — dois
  botões no cabeçalho apontando para `localhost:9090/targets` e para o dashboard no `localhost:3000`.

---

## 5. Como usar

### 5.1. Subida completa (recomendado)

```bash
cd app_build
./start-all.sh      # sobe Prometheus + Grafana (se houver Docker) e os 6 microsserviços
```

| Recurso | URL | Observação |
| :--- | :--- | :--- |
| Aplicação (Gateway) | http://localhost:8080 | ponto de entrada único |
| Eureka | http://localhost:8761 | — |
| Prometheus | http://localhost:9090 | alvos em `/targets` — os 6 devem estar **UP** |
| Grafana | http://localhost:3000 | login `admin`/`admin` |

Para encerrar tudo: `./stop-all.sh`.

### 5.2. Apenas a stack de observabilidade

```bash
cd app_build/observability
docker compose up -d      # sobe
docker compose down       # derruba (preserva os dados)
docker compose down -v    # derruba e apaga os volumes
```

Trocando as credenciais do Grafana:

```bash
GRAFANA_ADMIN_USER=prof GRAFANA_ADMIN_PASSWORD=senha123 docker compose up -d
```

### 5.3. Verificando a instrumentação

```bash
# métricas cruas de um serviço
curl http://localhost:8081/actuator/prometheus | head

# gerar tráfego e conferir o counter de negócio
curl -X POST http://localhost:8080/api/pecas -H 'Content-Type: application/json' \
     -d '{"numeroIdentificacao":"P-001","nome":"Parafuso","descricao":"teste"}'

# consultar no Prometheus (UI em :9090 → aba Graph)
pecas_cadastro_total{resultado="sucesso"}
```

### 5.4. Consultas PromQL úteis

```promql
# Requisições por segundo, por serviço
sum by (application) (rate(http_server_requests_seconds_count[1m]))

# Latência p95 por serviço
histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[1m])))

# Erros 5xx por serviço
sum by (application) (rate(http_server_requests_seconds_count{status=~"5.."}[1m]))

# Registros atuais por domínio
pecas_registros or clientes_registros or representantes_registros
```

### 5.5. Checklist de aceite (demonstração)

1. `curl` em `/actuator/prometheus` nas portas 8080–8083, 8761 e 8888 retorna métricas com a tag `application`.
2. `http://localhost:9090/targets` mostra os 6 alvos **UP**.
3. Um `POST /api/pecas` pelo Gateway incrementa `pecas_cadastro_total{resultado="sucesso"}`.
4. `http://localhost:3000` abre o dashboard "Microsserviços — Visão Geral" já com dados, sem configuração manual.

---

## 6. Observações e limitações conhecidas

- **Counters são efêmeros nos serviços**: reiniciar um microsserviço zera os contadores em memória. O
  histórico fica no Prometheus, e `rate()`/`increase()` tratam corretamente esses *resets*.
- **É necessário recompilar** (`mvn clean package`) os módulos após estas mudanças, pois foram alteradas
  dependências e código-fonte Java.
- **Os containers não iniciam a aplicação** — eles apenas observam os processos que rodam no host. Sem os
  microsserviços no ar, os alvos aparecem DOWN no Prometheus.
- **Fora de escopo neste ciclo**: logs estruturados centralizados e rastreamento distribuído (traces).

---

## 7. Troubleshooting

### "O `start-all.sh` sobe tudo, mas `localhost:9090` e `localhost:3000` não abrem"

Quase sempre significa que os **containers não subiram** — a aplicação Java sobe normalmente porque a
stack de observabilidade é opcional no script.

Diagnóstico:

```bash
docker info      # o daemon responde?
docker ps        # prometheus e grafana estão na lista?
```

**Causa mais comum: usuário fora do grupo `docker`.** O daemon está ativo, mas o socket
`/var/run/docker.sock` pertence a `root:docker` e o comando falha com
`permission denied while trying to connect to the docker API`.

Correção definitiva (exige logout/login ou `newgrp` para valer na sessão atual):

```bash
sudo usermod -aG docker "$USER"
newgrp docker            # aplica o grupo no shell atual
docker ps                # deve funcionar sem sudo agora
```

> Atenção: pertencer ao grupo `docker` equivale, na prática, a acesso root na máquina. Em ambiente
> compartilhado, prefira a alternativa com `sudo` abaixo.

Alternativa sem alterar grupos:

```bash
cd app_build/observability
sudo docker compose up -d
```

Outras causas possíveis:

| Sintoma | Causa | Correção |
| :--- | :--- | :--- |
| `Cannot connect to the Docker daemon` | daemon parado | `sudo systemctl start docker` |
| `port is already allocated` | 9090 ou 3000 em uso | `ss -ltnp \| grep -E '9090\|3000'` e libere a porta |
| Containers sobem, mas alvos ficam **DOWN** no `/targets` | microsserviços não estão no ar, ou `host.docker.internal` não resolveu | confira `curl localhost:8081/actuator/prometheus` e o `extra_hosts` do compose |
| Grafana abre sem dados | Prometheus sem métricas ainda | aguarde ~15s (scrape de 5s) e gere tráfego na aplicação |

### Melhoria aplicada nos scripts

`start-all.sh` e `stop-all.sh` passaram a testar `docker info` (e não apenas a presença do cliente
`docker`). Antes, `docker compose version` respondia com sucesso mesmo sem acesso ao daemon, o script
tentava subir a stack, falhava silenciosamente no meio do log e **ainda assim imprimia as URLs do
Prometheus e do Grafana** ao final — dando a impressão de que tudo havia subido. Agora o script avisa
explicitamente o motivo, sugere a correção e só anuncia as URLs quando os containers realmente sobem.
