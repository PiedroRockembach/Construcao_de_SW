# Construção de Software - Sistema de Microserviços Spring Cloud

Projeto da disciplina de Construção de Software (PUCRS), composto por uma arquitetura completa de microsserviços para gestão de **Peças**, **Clientes** e **Representantes Comerciais**.

---

## 🏛️ Estrutura do Repositório

- [`app_build/`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/): Código-fonte de todos os microsserviços, frontend integrado e scripts de execução.
- [`production_artifacts/`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/production_artifacts/): Especificações técnicas da arquitetura do sistema e da estratégia de testes automatizados.
- [Documentação Detalhada do Sistema](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/README.md): Instruções de inicialização, observabilidade e endpoints.

---

## 🧪 Suíte de Testes Automatizados

O projeto possui **63 testes unitários e de persistência desacoplada** com execução em milissegundos e sem necessidade de conexão externa ou banco de dados:

1. **Testes Unitários dos Serviços (`*ServiceTest`)**:
   - Isolamento com Mockito e `SimpleMeterRegistry`.
   - Validações de cadastro, unicidade, conflitos (`409 CONFLICT`), consultas e exceções (`404 NOT_FOUND`).
2. **Testes Unitários dos Controladores Isolando o Framework Web (`*ControllerTest`)**:
   - **POJO Puro**: Instanciação direta via `new Controller(mockService)` testando `ResponseEntity` sem Spring Web, Tomcat ou servlets.
   - **Standalone MockMvc**: Validação de rotas REST e JSON sem carga do contexto de aplicação.
3. **Código e Testes de Persistência Isolando JPA e Banco de Dados (`InMemory*Repository` / `*PersistenceTest`)**:
   - Implementações em memória baseadas puramente em coleções (`ConcurrentHashMap`, `AtomicLong`), sem Hibernate, JPA ou BD.
   - Validação da camada de persistência e demonstração do `Service` operando agnóstico à infraestrutura.

### Execução dos Testes Unitários:
```bash
cd app_build
mvn test
```

---

## 🧬 Testes de Mutação com Pitest

O projeto conta com testes de mutação utilizando o framework **Pitest (PIT)** integrado ao Maven para aferir a eficácia real das suítes de testes unitários:

- **Estatísticas Consolidadas**:
  - **45 mutantes gerados** e **45 mutantes eliminados (100% Mutation Score)**.
  - **0 mutantes sobreviventes**.
  - **100% de Test Strength** e **100% de Cobertura de Linha** nas classes alvo.
- **Classes Alvo Analisadas**: Camadas de negócio (`*Service`) e controladores REST (`*Controller`) de `pecas-service`, `clientes-service` e `representantes-service`.
- **Operadores de Mutação**: Conjunto `STRONGER` (condicionais de fronteira, inversões booleanas, valores de retorno, chamadas void e mutações aritméticas).

### Execução dos Testes de Mutação:
```bash
cd app_build
./run-mutation-tests.sh
```

Ou diretamente via Maven:
```bash
cd app_build
mvn test-compile org.pitest:pitest-maven:mutationCoverage -pl pecas-service,clientes-service,representantes-service
```

### 📊 Relatórios de Mutação:
- **Dashboard Geral**: [`app_build/mutation-dashboard.html`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/mutation-dashboard.html)
- **Peças**: [`app_build/pecas-service/target/pit-reports/index.html`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/pecas-service/target/pit-reports/index.html)
- **Clientes**: [`app_build/clientes-service/target/pit-reports/index.html`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/clientes-service/target/pit-reports/index.html)
- **Representantes**: [`app_build/representantes-service/target/pit-reports/index.html`](file:///home/fejunglau/Documents/PUCRS/Constru%C3%A7%C3%A3o/Construcao_de_SW/app_build/representantes-service/target/pit-reports/index.html)