# Roteiro de Demonstração — Requisições e as métricas que elas afetam

Roteiro prático para demonstrar a observabilidade do projeto: cada cenário traz a requisição, a resposta
esperada, **quais métricas mudam** e **onde observar** no Prometheus e no Grafana.

As requisições estão no formato de ferramentas como **Thunder Client**, **Postman** ou **Insomnia**: copie
o **método**, a **URL** e, quando houver, o **body** (em *Body → JSON*). O header
`Content-Type: application/json` é adicionado automaticamente por essas ferramentas ao escolher body JSON.

Todas as requisições passam pelo **API Gateway** (`http://localhost:8080`), como manda a arquitetura —
isso faz com que cada chamada apareça **duas vezes** nas métricas HTTP: uma no `gateway-service` e outra
no serviço de destino. Esse "efeito duplo" é, por si só, um bom ponto para mostrar durante a apresentação.

---

## 0. Preparação

No terminal:

```bash
cd app_build
./start-all.sh            # sobe observabilidade + os 6 microsserviços
```

Abra em abas separadas:

| Aba | URL | Para quê |
| :--- | :--- | :--- |
| Grafana | http://localhost:3000 → dashboard **Microsserviços — Visão Geral** | visão principal da demo |
| Prometheus — Targets | http://localhost:9090/targets | provar que os 6 alvos estão **UP** |
| Prometheus — Graph | http://localhost:9090/graph | rodar PromQL ao vivo |
| Aplicação | http://localhost:8080 | frontend, se quiser demonstrar pela UI |

> **Dica de ritmo:** o scrape é a cada **5s**. Depois de disparar uma requisição, espere ~5–10s antes de
> apontar o gráfico. No Grafana, deixe o *time range* em **Last 15 minutes** e o auto-refresh em **5s**.

**Estado inicial (opcional):** os contadores vivem em memória e zeram a cada restart do serviço. Se quiser
começar do zero para a demo, reinicie apenas os serviços de negócio antes de começar. Caso contrário,
apenas mostre os valores **variando** — é o que importa.

**Sugestão de organização na ferramenta:** crie uma *Collection* "Demo Observabilidade" com uma pasta por
cenário e uma variável de ambiente `baseUrl = http://localhost:8080`. Assim as URLs abaixo podem ser
coladas como `{{baseUrl}}/api/pecas`.

Confira que a coleta está saudável antes de começar:

**GET**
```
http://localhost:9090/api/v1/targets
```
Esperado: 7 ocorrências de `"health": "up"` na resposta (6 serviços + o próprio Prometheus).

---

## Cenário 1 — Cadastro com sucesso

A requisição mais importante da demo: mexe numa métrica **de negócio**, não só de infraestrutura.

**POST**
```
http://localhost:8080/api/pecas
```
**Body (JSON)**
```json
{
  "numeroIdentificacao": "PC-1001",
  "nome": "Parafuso Sextavado",
  "descricao": "Aço inox M8"
}
```

**Resposta esperada:** `201 Created` com o JSON da peça criada.

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `pecas_cadastro_total{resultado="sucesso"}` | **+1** |
| `pecas_registros` | **+1** (Gauge, lido do banco a cada scrape) |
| `http_server_requests_seconds_count{application="pecas-service",uri="/pecas",method="POST",status="201"}` | +1 |
| `http_server_requests_seconds_count{application="gateway-service",...}` | +1 (a mesma chamada, vista pelo Gateway) |
| `spring_cloud_gateway_requests_seconds_count{routeId="pecas-route"}` | +1 |

**Onde ver:** painéis *Cadastros (sucesso × conflito)* e *Registros na Base*, na linha **Métricas de Negócio**.

**PromQL:**
```promql
pecas_cadastro_total{resultado="sucesso"}
pecas_registros
```

Para ver o gráfico subir em degraus, envie mais algumas peças trocando apenas o `numeroIdentificacao`:

**POST** `http://localhost:8080/api/pecas`
```json
{ "numeroIdentificacao": "PC-1002", "nome": "Porca M8", "descricao": "Aço carbono" }
```

**POST** `http://localhost:8080/api/pecas`
```json
{ "numeroIdentificacao": "PC-1003", "nome": "Arruela Lisa", "descricao": "Zincada 8mm" }
```

**POST** `http://localhost:8080/api/pecas`
```json
{ "numeroIdentificacao": "PC-1004", "nome": "Rolamento 6202", "descricao": "Blindado 2RS" }
```

> **Atalho no Postman:** no body, use `"numeroIdentificacao": "PC-{{$randomInt}}"` e clique *Send* várias
> vezes — cada envio gera um número novo. No Thunder Client, o equivalente é `"PC-{{#number}}"`.

---

## Cenário 2 — Cadastro em conflito (regra de negócio rejeitando)

Envie **exatamente** o mesmo cadastro do Cenário 1:

**POST**
```
http://localhost:8080/api/pecas
```
**Body (JSON)**
```json
{
  "numeroIdentificacao": "PC-1001",
  "nome": "Parafuso Sextavado",
  "descricao": "Aço inox M8"
}
```

**Resposta esperada:** `409 Conflict` — "Já existe uma peça cadastrada com o número de identificação: PC-1001".

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `pecas_cadastro_total{resultado="conflito"}` | **+1** |
| `pecas_registros` | **não muda** — nada foi gravado |
| `http_server_requests_seconds_count{status="409",outcome="CLIENT_ERROR"}` | +1 |
| Taxa de erros 4xx | sobe |

**O que dizer na apresentação:** é o mesmo endpoint e a mesma métrica `pecas_cadastro_total`, separados
apenas pela **tag** `resultado`. Uma tag bem escolhida transforma um contador em uma análise — dá para
perguntar "que fração das tentativas de cadastro está falhando?":

```promql
sum(rate(pecas_cadastro_total{resultado="conflito"}[5m]))
  / sum(rate(pecas_cadastro_total[5m]))
```

---

## Cenário 3 — Consulta não encontrada (404)

**GET**
```
http://localhost:8080/api/pecas/999999
```

**GET**
```
http://localhost:8080/api/clientes/cpf/00000000000
```

**Resposta esperada:** `404 Not Found` nas duas.

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `pecas_consulta_nao_encontrada_total` | **+1** |
| `clientes_consulta_nao_encontrada_total` | **+1** |
| `http_server_requests_seconds_count{status="404"}` | +1 em cada serviço |

**Onde ver:** painel *Consultas Não Encontradas (404) — últimos 5 min*.

**Por que essa métrica existe:** um 404 isolado é normal (usuário digitou errado); um **pico** de 404 pode
indicar cliente integrado com dados errados ou base corrompida. O contador transforma o evento em série
temporal, permitindo enxergar a tendência.

---

## Cenário 4 — Erro de validação (400) — o contraste

**POST**
```
http://localhost:8080/api/clientes
```
**Body (JSON)**
```json
{
  "cpf": "123",
  "nome": "X"
}
```

**Resposta esperada:** `400 Bad Request` (CPF fora do padrão e nome com menos de 2 caracteres).

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `http_server_requests_seconds_count{status="400"}` | +1 |
| `clientes_cadastro_total` | **não muda** |
| `clientes_registros` | **não muda** |

**O que dizer:** a validação acontece **antes** da camada de Service, onde está a instrumentação. Por isso
a requisição aparece nas métricas HTTP automáticas, mas não nos contadores de negócio. Isso mostra que os
dois níveis de métrica são complementares: um vê *todo* o tráfego, o outro vê *decisões de domínio*.

---

## Cenário 5 — Os três domínios em ação

Para encher todos os painéis de negócio de uma vez. Envie as requisições na ordem:

**5.1 — Cliente novo** → `201`

**POST** `http://localhost:8080/api/clientes`
```json
{
  "cpf": "111.222.333-44",
  "nome": "Maria Silva"
}
```

**5.2 — Mesmo cliente de novo** → `409`

**POST** `http://localhost:8080/api/clientes`
```json
{
  "cpf": "111.222.333-44",
  "nome": "Maria Silva"
}
```

**5.3 — Representante novo** → `201`

**POST** `http://localhost:8080/api/representantes`
```json
{
  "cpf": "555.666.777-88",
  "nome": "João Souza"
}
```

**5.4 — Mesmo representante de novo** → `409`

**POST** `http://localhost:8080/api/representantes`
```json
{
  "cpf": "555.666.777-88",
  "nome": "João Souza"
}
```

**5.5 — Consulta de cliente existente** → `200`

**GET**
```
http://localhost:8080/api/clientes/cpf/111.222.333-44
```

**5.6 — Listagem de representantes** → `200`

**GET**
```
http://localhost:8080/api/representantes
```

**Métricas afetadas:** `clientes_cadastro_total` e `representantes_cadastro_total` (sucesso e conflito),
`clientes_registros`, `representantes_registros`, além do tráfego HTTP dos três serviços e do Gateway.

**Onde ver:** os painéis de negócio passam a exibir **três séries** cada; use a variável `application` no
topo do dashboard para filtrar um serviço por vez e mostrar como a tag comum organiza tudo.

---

## Cenário 6 — Carga contínua: taxa de requisições e latência p95

Este é o cenário que "dá vida" aos gráficos de linha. Crie uma pasta **"Carga"** na collection com estas
requisições:

**GET**
```
http://localhost:8080/api/pecas
```

**GET**
```
http://localhost:8080/api/clientes
```

**GET**
```
http://localhost:8080/api/representantes
```

**GET**
```
http://localhost:8080/api/pecas/busca?nome=Parafuso
```

**GET** *(404 proposital)*
```
http://localhost:8080/api/pecas/999999
```

Depois execute a pasta em repetição:

- **Postman:** botão direito na pasta → *Run folder* → **Iterations: 200**, **Delay: 50 ms** → *Run*.
- **Thunder Client:** botão direito na pasta → *Run All* → em *Options*, **Iterations: 200**,
  **Delay: 50 ms** (o Collection Runner com iterações é recurso da versão paga; na gratuita, clique
  *Run All* algumas vezes seguidas ou use o Postman para esta etapa).

Isso gera um fluxo de alguns req/s por 1–2 minutos.

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `http_server_requests_seconds_count` | cresce continuamente → `rate()` estabiliza num patamar |
| `http_server_requests_seconds_bucket` | alimenta o cálculo de **p95** |
| `spring_cloud_gateway_requests_seconds_count{routeId=...}` | tráfego por rota no Gateway |
| `jvm_memory_used_bytes`, `process_cpu_usage` | sobem junto com a carga |
| `pecas_consulta_nao_encontrada_total` | cresce de forma constante |

**Onde ver:** praticamente o dashboard inteiro — *Taxa de Requisições*, *Latência p95*, *Taxa de Erros*,
*Gateway — Requisições por Rota*, *Memória Heap*, *CPU do Processo*.

**PromQL:**
```promql
sum by (application) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[1m])))
sum by (routeId) (rate(spring_cloud_gateway_requests_seconds_count[1m]))
```

**O que dizer:** os contadores são sempre crescentes; quem responde "quanto está acontecendo **agora**" é a
função `rate()`. E a latência p95 só pode ser calculada porque habilitamos
`percentiles-histogram` no `application.yml` — sem os buckets, o `histogram_quantile()` não teria dados.

---

## Cenário 7 — Queda de um serviço (o momento mais convincente)

Derrubar o serviço é uma ação de terminal, não uma requisição HTTP:

```bash
fuser -k 8081/tcp          # mata o pecas-service
```

Agora, na ferramenta, chame o endpoint pelo Gateway:

**GET**
```
http://localhost:8080/api/pecas
```

**Resposta esperada:** `503 Service Unavailable` — o Gateway não encontra instância registrada.

**Métricas afetadas:**

| Métrica | Efeito |
| :--- | :--- |
| `up{application="pecas-service"}` | **1 → 0** em até 5s |
| `http_server_requests_seconds_count{application="gateway-service",status="503"}` | +1 |
| Todas as métricas do `pecas-service` | **param de ser atualizadas** (sem novos scrapes) |

**Onde ver:** painel *Status dos Serviços* fica vermelho; *Taxa de Erros HTTP* do Gateway dispara;
em http://localhost:9090/targets o alvo `pecas-service` fica **DOWN** com o erro de conexão.

Para restaurar, no terminal:

```bash
cd app_build
java -jar pecas-service/target/pecas-service-1.0.0-SNAPSHOT.jar > logs/pecas-service.log 2>&1 &
```

Aguarde ~30s (registro no Eureka) e repita o **GET** acima — volta a responder `200`.

**O que dizer:** repare que ao voltar, `pecas_cadastro_total` recomeça do zero — contadores são
**efêmeros no processo**. O histórico continua no Prometheus, e é por isso que se usa `rate()`/`increase()`
em vez do valor bruto: essas funções detectam e tratam o *reset* do contador.

---

## Cenário 8 — Métricas de infraestrutura (Config Server e Eureka)

Mostra que a observabilidade cobre os **6** módulos, não só os de negócio:

**GET** *(Config Server)*
```
http://localhost:8888/actuator/health
```

**GET** *(Eureka Dashboard)*
```
http://localhost:8761/
```

**Métricas afetadas:** `http_server_requests_seconds_count{application="config-server"}` e
`{application="discovery-server"}`, além das métricas de JVM dos dois.

**PromQL:**
```promql
sum by (application) (rate(http_server_requests_seconds_count[1m]))
# ative a variável "application" e selecione config-server e discovery-server
```

---

## Extra — Ver as métricas cruas pela ferramenta

Útil para mostrar o formato de texto que o Prometheus coleta:

**GET**
```
http://localhost:8081/actuator/prometheus
```

Procure na resposta (Ctrl+F) por `pecas_cadastro_total` e `pecas_registros` — são os valores exatos que o
Prometheus lê a cada 5s. Troque a porta para ver os outros serviços: `8080` Gateway, `8082` clientes,
`8083` representantes, `8761` Eureka, `8888` Config Server.

---

## Roteiro sugerido (≈ 8 minutos)

| Tempo | Passo |
| :--- | :--- |
| 0:00 | Mostrar `/targets` com os 6 alvos **UP** — "estamos coletando de todos os módulos" |
| 0:30 | **Extra** — `GET /actuator/prometheus` e mostrar o formato cru das métricas |
| 1:00 | **Cenário 6** (Runner em repetição) em background — deixar os gráficos se formando |
| 2:00 | **Cenário 1** — cadastro com sucesso, apontar o degrau no painel de negócio |
| 3:00 | **Cenário 2** — conflito, explicar a tag `resultado` |
| 4:00 | **Cenário 4** — 400 que não aparece nas métricas de negócio (métricas complementares) |
| 5:00 | Latência p95 e requisições por rota no Gateway |
| 6:00 | **Cenário 7** — matar o `pecas-service`, painel vermelho, 503 no Gateway |
| 7:00 | Restaurar o serviço, mostrar recuperação e explicar o reset dos contadores |

---

## Tabela-resumo: requisição → métrica

| Método | URL | Body | Status | Métricas de negócio | Métricas HTTP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| POST | `/api/pecas` | peça nova | 201 | `pecas_cadastro_total{sucesso}` +1, `pecas_registros` +1 | `status="201"` |
| POST | `/api/pecas` | peça repetida | 409 | `pecas_cadastro_total{conflito}` +1 | `status="409"`, erro 4xx |
| GET | `/api/pecas/999999` | — | 404 | `pecas_consulta_nao_encontrada_total` +1 | `status="404"`, erro 4xx |
| GET | `/api/clientes/cpf/00000000000` | — | 404 | `clientes_consulta_nao_encontrada_total` +1 | `status="404"` |
| POST | `/api/clientes` | CPF inválido | 400 | — *(nenhuma)* | `status="400"` |
| GET | `/api/pecas` | — | 200 | — | `status="200"`, latência, p95 |
| GET | `/api/pecas` *(serviço fora)* | — | 503 | — | `up` → 0, `status="503"` no Gateway |
| GET | `:8888/actuator/health` | — | 200 | — | tráfego do `config-server` |

*(URLs relativas a `http://localhost:8080`, exceto quando indicada outra porta.)*

---

## Observações

- Os exemplos foram **validados** contra a aplicação rodando: os contratos, os caminhos das rotas
  (`/api/{pecas,clientes,representantes}` com `StripPrefix=1`) e os códigos de status conferem.
- O CPF aceita os dois formatos: `111.222.333-44` ou `11122233344` (11 dígitos).
- Se um `POST` retornar 409 logo na primeira tentativa, o registro já existe de uma execução anterior —
  troque o `numeroIdentificacao`/`cpf` ou reinicie o serviço para limpar a base em memória.
- Detalhes de arquitetura, decisões e configuração estão em [explicacao.md](explicacao.md).
