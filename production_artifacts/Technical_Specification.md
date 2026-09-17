# Technical Specification: Testes Unitários de Serviços, Controllers e Persistência Desacoplada

## 1. Executive Summary
Esta especificação técnica estabelece a arquitetura, metodologia e implementação de **Testes Unitários Automatizados** para o ecossistema de microsserviços (`pecas-service`, `clientes-service` e `representantes-service`), com foco em três pilares fundamentais de isolamento:

1. **Testes Unitários da Camada de Serviços (`*Service`)**:
   - Validação da lógica de negócio e regras de validação/conflito.
   - Isolamento total de banco de dados e observabilidade externa através de dublês de teste (*Mocks* com Mockito).
2. **Testes Unitários da Camada de Controladores (`*Controller`) Isolando o Framework Web**:
   - Testes puros em POJO (*Plain Old Java Object*), instanciando os controllers diretamente via construtor e injetando serviços mockados.
   - Execução sem subir o contexto do Spring Boot, sem container Servlet/Tomcat e sem dependência do ciclo de vida web, validando o retorno do `ResponseEntity`, códigos de status HTTP e payloads.
   - Suporte complementar a testes *standalone* com `MockMvcBuilders.standaloneSetup()` para validação de contratos de rotas sem inicialização de contexto de infraestrutura.
3. **Código e Testes de Persistência Isolando o Framework de Persistência e o BD**:
   - Implementação do padrão de portas de persistência e repositórios em memória (*In-Memory Fake Repositories* / *Persistence Adapters*).
   - Isolamento completo do Spring Data JPA, Hibernate e de bancos de dados relacionais (mesmo H2 em memória), utilizando estruturas de dados nativas da linguagem Java (`Map`, `ConcurrentHashMap`, `AtomicLong`).
   - Testes unitários das operações de persistência e validação de contratos de consulta (`findBy...`, `existsBy...`, `save`, `findById`) puramente em memória e em microssegundos.

---

## 2. Requirements & Scope

### 2.1. Requisitos Funcionais

#### 2.1.1. Configuração de Dependências de Teste (Pom.xml)
- **RF-TEST-01**: Garantir no `pom.xml` pai (`app_build/pom.xml`) e nos módulos filhos (`pecas-service`, `clientes-service`, `representantes-service`) a inclusão da dependência `spring-boot-starter-test` em escopo `test`.
  - Provê: **JUnit Jupiter (JUnit 5)**, **Mockito**, **AssertJ**.

#### 2.1.2. Testes Unitários de Serviços (`*ServiceTest`)
- **RF-SERV-01 - Testes do `PecaService`**:
  - `cadastrar_Sucesso`: Deve salvar a entidade, incrementar o contador de métricas e retornar o DTO de resposta.
  - `cadastrar_ConflitoNumeroIdentificacao`: Quando o número de identificação já existir, deve lançar `ResponseStatusException` com status `409 CONFLICT` sem salvar a entidade.
  - `listarTodas_Sucesso`: Deve retornar lista de `PecaResponseDTO` mapeada a partir das entidades do repositório.
  - `buscarPorId_Sucesso`: Deve retornar DTO quando ID existir.
  - `buscarPorId_NaoEncontrado`: Deve lançar `ResponseStatusException` com status `404 NOT_FOUND` quando ID não for localizado.
  - `buscarPorNumeroIdentificacao_Sucesso`: Deve retornar DTO correspondente.
  - `buscarPorNumeroIdentificacao_NaoEncontrado`: Deve lançar `ResponseStatusException` com status `404 NOT_FOUND`.
  - `buscarPorNome_Sucesso`: Deve retornar lista filtrada por nome.
- **RF-SERV-02 - Testes do `ClienteService`**:
  - `cadastrar_Sucesso`: Cadastro com CPF e nome válidos, salvando com CPF sanitizado (trimmed) e incrementando métrica.
  - `cadastrar_ConflitoCpf`: Deve lançar `409 CONFLICT` quando CPF já existir.
  - `listarTodos_Sucesso`: Listagem completa.
  - `buscarPorId_Sucesso` / `buscarPorId_NaoEncontrado`: Retorno do DTO ou lançamento de `404 NOT_FOUND`.
  - `buscarPorCpf_Sucesso` / `buscarPorCpf_NaoEncontrado`: Consulta por CPF ou lançamento de `404 NOT_FOUND`.
  - `buscarPorNome_Sucesso`: Consulta por nome parcial.
- **RF-SERV-03 - Testes do `RepresentanteService`**:
  - `cadastrar_Sucesso`: Cadastro com CPF e nome válidos, salvando com CPF sanitizado e incrementando métrica.
  - `cadastrar_ConflitoCpf`: Deve lançar `409 CONFLICT` quando CPF já existir.
  - `listarTodos_Sucesso`: Listagem de todos os representantes.
  - `buscarPorId_Sucesso` / `buscarPorId_NaoEncontrado`: DTO ou `404 NOT_FOUND`.
  - `buscarPorCpf_Sucesso` / `buscarPorCpf_NaoEncontrado`: DTO ou `404 NOT_FOUND`.
  - `buscarPorNome_Sucesso`: Filtro parcial por nome.

#### 2.1.3. Testes Unitários de Controllers Isolando o Framework Web (`*ControllerTest`)
- **RF-CTRL-01 - Isolamento Estrito do Framework Web (Pure POJO Unit Tests)**:
  - Os controladores (`PecaController`, `ClienteController`, `RepresentanteController`) devem ser instanciados de forma pura com `new Controller(mockService)`.
  - Nenhuma anotação de subida de servidor (`@SpringBootTest`, `@WebMvcTest`) deve ser necessária na suíte unitária pura.
  - Os testes devem verificar que a chamada ao método do controller aciona o método correspondente do serviço mockado e devolve `ResponseEntity` com status correto (`201 CREATED` para cadastro, `200 OK` para consultas) e o corpo de resposta esperado.
- **RF-CTRL-02 - Standalone MockMvc (Sem Contexto de Servidor)**:
  - Para validação de serialização JSON e mapeamento de rota HTTP sem carregar o Spring ApplicationContext, os testes utilizarão `MockMvcBuilders.standaloneSetup(controller).build()`.
  - Garante validação web desacoplada de container servlet / servidor de aplicação.

#### 2.1.4. Isolamento da Persistência e Banco de Dados (`*PersistenceTest` & `InMemory*Repository`)
- **RF-PERS-01 - Desacoplamento da Camada de Persistência**:
  - Criação de uma interface de domínio / porta de repositório ou adaptação do repositório para desacoplamento direto do Spring Data JPA / Hibernate.
  - Implementação de repositórios em memória (*In-Memory Fakes*):
    - `InMemoryPecaRepository`
    - `InMemoryClienteRepository`
    - `InMemoryRepresentanteRepository`
  - Utilização de coleções em memória (`Map<Long, T>`, `AtomicLong` para IDs) para gerenciar o estado sem conexão JDBC, sem banco de dados relacional e sem necessidade do Hibernate.
- **RF-PERS-02 - Testes Unitários de Persistência**:
  - Testes unitários das implementações de persistência garantindo que as operações de CRUD e queries customizadas (`findByNumeroIdentificacao`, `findByCpf`, `findByNomeContainingIgnoreCase`, `existsBy...`) funcionam deterministicamente sem envolver banco de dados ou framework ORM.
  - Demonstração de execução do `Service` com o `InMemoryRepository` diretamente, confirmando que a camada de negócio opera de forma agnóstica à infraestrutura de banco de dados.

---

### 2.2. Requisitos Não-Funcionais
- **RNF-TEST-01 - Velocidade de Execução**: Como testes unitários puros sem subida de contexto Spring Boot ou inicialização de banco de dados, todos os testes de cada microsserviço devem executar em menos de 2 segundos no total.
- **RNF-TEST-02 - Determinismo e Isolamento**: Nenhum teste deve compartilhar estado ou depender de ordem de execução. Não há necessidade de portas de rede abertas ou containers Docker rodando.
- **RNF-TEST-03 - Padrão de Nomenclatura e Organização**:
  - Estrutura de pastas espelhada em `src/test/java/br/pucrs/construcao/...`.
  - Convenção AAA (*Arrange, Act, Assert*) ou BDD (*Given, When, Then*).

---

## 3. Architecture & Tech Stack

```
+---------------------------------------------------------------------------------------+
|                                    TEST SUITE                                         |
+---------------------------------------------------------------------------------------+
        |                                   |                                  |
        v                                   v                                  v
+-----------------------+       +-----------------------+      +-----------------------+
|  Controller Unit Test |       |   Service Unit Test   |      | Persistence Unit Test |
|  (Web Framework Free) |       | (Infra/DB Mocked Out) |      | (No JPA / No DB Engine)|
+-----------------------+       +-----------------------+      +-----------------------+
        |                                   |                                  |
        | instantiates with 'new'           | instantiates with 'new'          | instantiates with 'new'
        v                                   v                                  v
+-----------------------+       +-----------------------+      +-----------------------+
|    *Controller.java   |       |     *Service.java     |      |  InMemory*Repository  |
| (Calls Mock Service)  |       | (Calls Mock Repository|      | (Pure Map / Memory)   |
| (Asserts ResponseEntity)      |  & Mock MeterRegistry)|      | (Pure Java Logic)     |
+-----------------------+       +-----------------------+      +-----------------------+
```

### 3.1. Tecnologias Empregadas

| Componente | Tecnologia / Ferramenta | Finalidade |
| :--- | :--- | :--- |
| **Runner de Testes** | JUnit Jupiter 5.10.x | Framework de testes unitários padrão do Spring Boot 3.2.5 |
| **Mocks & Spies** | Mockito 5.x (`mockito-core`, `mockito-junit-jupiter`) | Criação de dublês de teste, isolamento de dependências |
| **Asserções Fluentes** | AssertJ (`org.assertj.core.api.Assertions`) | Asserções claras, legíveis e ricas (`assertThat`) |
| **Standalone Web Slice** | Spring Test MockMvc (`standaloneSetup`) | Teste isolado do controller sem contexto de aplicação |
| **Persistência Fake** | Java Collections Framework (`ConcurrentHashMap`, `AtomicLong`) | Simulação de persistência sem JPA e sem BD |

---

## 4. Estrutura de Arquivos e Novos Artefatos de Código

### 4.1. Modificações de Dependências
- `app_build/pom.xml`: Adicionar `spring-boot-starter-test` no `<dependencies>` global para disponibilização automática em todos os serviços filhos.

### 4.2. Novos Arquivos de Teste no `pecas-service`
1. `src/test/java/br/pucrs/construcao/pecas/service/PecaServiceTest.java`:
   - Teste unitário de `PecaService` isolando `PecaRepository` e `MeterRegistry`.
2. `src/test/java/br/pucrs/construcao/pecas/controller/PecaControllerTest.java`:
   - Teste unitário de `PecaController` isolando completamente o framework web (teste POJO puro e standalone MockMvc).
3. `src/test/java/br/pucrs/construcao/pecas/persistence/InMemoryPecaRepository.java`:
   - Implementação de persistência em memória sem dependência de JPA ou banco de dados.
4. `src/test/java/br/pucrs/construcao/pecas/persistence/PecaPersistenceTest.java`:
   - Teste unitário da camada de persistência isolando framework JPA e BD.

### 4.3. Novos Arquivos de Teste no `clientes-service`
1. `src/test/java/br/pucrs/construcao/clientes/service/ClienteServiceTest.java`:
   - Teste unitário de `ClienteService` isolando `ClienteRepository` e `MeterRegistry`.
2. `src/test/java/br/pucrs/construcao/clientes/controller/ClienteControllerTest.java`:
   - Teste unitário de `ClienteController` isolando o framework web.
3. `src/test/java/br/pucrs/construcao/clientes/persistence/InMemoryClienteRepository.java`:
   - Implementação de persistência em memória de clientes.
4. `src/test/java/br/pucrs/construcao/clientes/persistence/ClientePersistenceTest.java`:
   - Teste unitário da lógica de persistência isolada.

### 4.4. Novos Arquivos de Teste no `representantes-service`
1. `src/test/java/br/pucrs/construcao/representantes/service/RepresentanteServiceTest.java`:
   - Teste unitário de `RepresentanteService` isolando `RepresentanteRepository` e `MeterRegistry`.
2. `src/test/java/br/pucrs/construcao/representantes/controller/RepresentanteControllerTest.java`:
   - Teste unitário de `RepresentanteController` isolando o framework web.
3. `src/test/java/br/pucrs/construcao/representantes/persistence/InMemoryRepresentanteRepository.java`:
   - Implementação de persistência em memória de representantes.
4. `src/test/java/br/pucrs/construcao/representantes/persistence/RepresentantePersistenceTest.java`:
   - Teste unitário da lógica de persistência isolada.

---

## 5. Fluxo de Execução e Verificação

1. **Compilação e Execução dos Testes**:
   - `mvn clean test` executado na raiz (`app_build`) ou em cada microsserviço individualmente.
   - Todos os testes devem rodar em ambiente *offline* (sem dependência de banco de dados, Eureka ou Docker ativos).
2. **Critérios de Aceite**:
   - 100% dos testes unitários passando (`BUILD SUCCESS`).
   - Cobertura das classes de Service, Controller e Persistência isolada.
   - Nenhuma conexão externa ou porta aberta requisitada durante o ciclo de teste.
