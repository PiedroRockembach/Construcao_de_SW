# Technical Specification: Sistema de Microserviços de Peças, Clientes e Representantes

## 1. Executive Summary
O sistema consiste em uma arquitetura orientada a microsserviços desenvolvida com o ecossistema **Spring Cloud** e **Spring Boot 3 (Java 17)**. A solução gerencia o ciclo de vida e operações de **Peças**, **Clientes** e **Representantes Comerciais**, garantindo alta coesão, baixo acoplamento, resiliência e facilidade de escala.

O ecossistema implementa rigorosamente os padrões solicitados:
- **Centralized Configuration**: Spring Cloud Config Server provendo parâmetros centralizados por perfil.
- **Service Discovery & Registry**: Spring Cloud Netflix Eureka Server para registro dinâmico e localização transparente dos nós de serviço.
- **API Gateway**: Spring Cloud Gateway como único ponto de entrada para clientes e frontend, com roteamento dinâmico baseado no Eureka, balanceamento de carga e configuração CORS.
- **Frontend / Client Testing**: Interface Web SPA intuitiva servida pelo próprio Gateway (ou acessível via navegador) e coleção Postman para testes integrados direcionados exclusivamente ao Gateway (`http://localhost:8080`).

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
