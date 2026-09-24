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

## 📊 Observabilidade: Métricas com Prometheus e Grafana

O projeto inclui suporte nativo a métricas via **Spring Boot Actuator** e **Micrometer Prometheus**, suportados por containers Docker.

### 🐳 Containers de Suporte
- **Prometheus** (`porta 9090`): Realiza scraping a cada 5 segundos nos endpoints `/actuator/prometheus` de todos os serviços.
- **Grafana** (`porta 3000`): Painéis em tempo real com datasource e dashboard provisionados automaticamente.

### 🔗 Pontos de Acesso de Monitoramento:
- **Painel Grafana**: 👉 **[http://localhost:3000](http://localhost:3000)** (Login padrão: `admin` / `admin`)
  - *Dashboard Pré-carregado*: Navegue em **Dashboards** > pasta **Microserviços** > **Microserviços Spring Cloud - Dashboard Geral** ou acesse diretamente: [http://localhost:3000/d/spring-microservices](http://localhost:3000/d/spring-microservices).
- **Console Prometheus**: 👉 **[http://localhost:9090](http://localhost:9090)**
  - Verifique os alvos monitorados em: [http://localhost:9090/targets](http://localhost:9090/targets).

### 🚀 Comandos Dedicados de Monitoramento:
- **Iniciar apenas Prometheus e Grafana**:
  ```bash
  cd app_build
  ./start-monitoring.sh
  ```
- **Parar Prometheus e Grafana**:
  ```bash
  cd app_build
  ./stop-monitoring.sh
  ```

### 📈 Métricas Customizadas Disponíveis:
- `pecas_created_total`: Contador de peças cadastradas com sucesso.
- `clientes_created_total`: Contador de clientes cadastrados com sucesso.
- `representantes_created_total`: Contador de representantes cadastrados com sucesso.
- `http_server_requests_seconds_count` e `_sum`: Taxa de requisições e latência média por rota e status HTTP.
- `jvm_memory_used_bytes` / `jvm_threads_live_threads`: Métricas de saúde e recursos da JVM.

---

## 🧪 Testes Automatizados e Estratégia de Isolamento

O projeto implementa uma suíte com **63 testes unitários e de persistência desacoplada**, cobrindo os três microsserviços de negócio (`pecas-service`, `clientes-service` e `representantes-service`) com isolamento estrito:

### 1. Testes Unitários dos Serviços (`*ServiceTest`)
- **Isolamento Total de Infraestrutura**: O banco de dados e os contadores de observabilidade são isolados com **Mockito** e registros em memória (`SimpleMeterRegistry`).
- **Cenários Cobertos**:
  - Criação de entidades e incremento das métricas de negócio.
  - Conflito e exceções de regras de negócio (`ResponseStatusException(409 CONFLICT)`) para duplicidade de CPF ou código de identificação.
  - Consulta por ID, CPF/código e busca textual parcial com verificação de `ResponseStatusException(404 NOT_FOUND)`.

### 2. Testes Unitários dos Controladores Isolando o Framework Web (`*ControllerTest`)
- **POJO Puro (Isolamento Estrito do Framework Web)**:
  - Os controladores são instanciados diretamente via construtor Java (`new Controller(mockService)`), sem inicializar o contexto do Spring Boot, sem Tomcat/Netty e sem container de servlet.
  - Validação direta das chamadas aos métodos, retorno de `ResponseEntity`, status HTTP (`201 CREATED`, `200 OK`) e integridade do payload retornado.
- **Standalone MockMvc**:
  - Testes isolados de endpoints HTTP e serialização JSON utilizando `MockMvcBuilders.standaloneSetup(controller)`, sem carga de contexto de aplicação.

### 3. Código e Testes de Persistência Isolando o Framework (JPA/Hibernate) e o Banco de Dados
- **Implementações In-Memory Fakes**:
  - `InMemoryPecaRepository`, `InMemoryClienteRepository` e `InMemoryRepresentanteRepository`.
  - Persistência operando puramente sobre coleções em memória (`ConcurrentHashMap`, `AtomicLong`), sem dependência de JDBC, Hibernate ou bancos de dados relacionais (mesmo H2).
- **Testes Unitários de Persistência (`*PersistenceTest`)**:
  - Validação das operações de persistência: geração de IDs, consultas customizadas (`findByCpf`, `findByNumeroIdentificacao`, `findByNomeContainingIgnoreCase`, `existsBy...`, `deleteById`).
  - Teste de integração do próprio `Service` executando em conjunto com o repositório em memória, comprovando desacoplamento total da regra de negócio em relação ao mecanismo de persistência.

### 🚀 Como Executar os Testes Unitários

- **Executar todos os testes do ecossistema**:
  ```bash
  cd app_build
  mvn test
  ```

- **Executar testes de um microsserviço individual**:
  ```bash
  cd app_build
  mvn test -pl pecas-service
  mvn test -pl clientes-service
  mvn test -pl representantes-service
  ```

---

## 🧬 Testes de Mutação com Pitest (Mutation Testing)

Para avaliar a qualidade e a eficácia das suítes de testes unitários, o projeto conta com integração ao framework **PIT (Pitest)** e o plugin oficial para **JUnit 5**, operando sobre **Java 17**.

### 🎯 Resultados e Indicadores
- **Total de Mutantes Gerados**: **45**
- **Mutantes Eliminados (Killed)**: **45** (**100% de Taxa de Mutação**)
- **Mutantes Sobreviventes (Survived)**: **0**
- **Test Strength**: **100%**
- **Line Coverage (Classes Mutadas)**: **100%**

| Microsserviço | Classes Analisadas | Mutantes Gerados | Eliminados | Sobreviventes | Score |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **`pecas-service`** | `PecaService`, `PecaController` | 15 | 15 | 0 | **100%** |
| **`clientes-service`** | `ClienteService`, `ClienteController` | 15 | 15 | 0 | **100%** |
| **`representantes-service`** | `RepresentanteService`, `RepresentanteController` | 15 | 15 | 0 | **100%** |

### 🚀 Como Executar os Testes de Mutação

- **Executar em todos os microsserviços via script unificado**:
  ```bash
  cd app_build
  ./run-mutation-tests.sh
  ```

- **Executar em um microsserviço específico via script**:
  ```bash
  cd app_build
  ./run-mutation-tests.sh pecas-service
  # ou clientes-service / representantes-service
  ```

- **Executar via Maven CLI**:
  ```bash
  cd app_build
  mvn test-compile org.pitest:pitest-maven:mutationCoverage -pl pecas-service,clientes-service,representantes-service
  ```

### 📊 Relatórios HTML Interativos
Após a execução, os relatórios ficam disponíveis em:
- **Painel Geral de Mutação**: [app_build/mutation-dashboard.html](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/mutation-dashboard.html)
- **`pecas-service`**: [pecas-service/target/pit-reports/index.html](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/pecas-service/target/pit-reports/index.html)
- **`clientes-service`**: [clientes-service/target/pit-reports/index.html](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/clientes-service/target/pit-reports/index.html)
- **`representantes-service`**: [representantes-service/target/pit-reports/index.html](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/representantes-service/target/pit-reports/index.html)

