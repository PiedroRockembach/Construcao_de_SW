# Technical Specification: Testes de Mutação com Pitest nos Microsserviços

## 1. Executive Summary
Esta especificação técnica estabelece a arquitetura, metodologia e implementação de **Testes de Mutação** utilizando o framework **PIT (Pitest)** no ecossistema de microsserviços Java (`pecas-service`, `clientes-service` e `representantes-service`).

Enquanto a cobertura tradicional de código (Line/Branch Coverage) apenas mede quais linhas foram executadas pelos testes unitários, os **testes de mutação** avaliam a *eficácia real* dos testes. O Pitest injeta pequenas falhas sintéticas no bytecode compilado (mutantes) — como inverter condicionais, alterar valores de retorno ou substituir operadores matemáticos — e executa os testes para garantir que pelo menos um teste falhe (matando o mutante). Mutantes que sobrevivem revelam brechas e asserções fracas nos testes.

O objetivo deste ciclo é:
1. Integrar o `pitest-maven` e a extensão `pitest-junit5-plugin` compatíveis com **Java 17** e **JUnit 5 (Jupiter)** no Maven Parent e nos microsserviços.
2. Configurar o escopo de mutação direcionado às classes de negócio (`service`), controladores (`controller`) e persistência desacoplada (`persistence`), com exclusão de classes sem lógica de negócio ativa (ex: DTOs puros, configurações e classes principais Spring Boot).
3. Gerar relatórios HTML interativos e determinísticos (`target/pit-reports/index.html`).
4. Auditar e fortalecer os testes unitários existentes para atingir uma pontuação de mutação (*Mutation Score*) superior a **80%**.

---

## 2. Requirements & Scope

### 2.1. Requisitos Funcionais

#### 2.1.1. Configuração do Maven Parent (`app_build/pom.xml`)
- **RF-PIT-01**: Declarar o plugin `org.pitest:pitest-maven` (versão estável `1.15.8`) e a dependência de plugin `org.pitest:pitest-junit5-plugin` (versão `1.2.1`) no bloco `<pluginManagement>` do `pom.xml` pai.
- **RF-PIT-02**: Configurar parâmetros globais herdáveis recomendados:
  - `<timestampedReports>false</timestampedReports>` para manter caminhos fixos de relatórios.
  - `<outputFormats><outputFormat>HTML</outputFormat><outputFormat>XML</outputFormat></outputFormats>` para visualização humana e integração com ferramentas de análise.
  - `<threads>4</threads>` para execução paralela otimizada de mutantes.
  - `<mutators><mutator>STRONGER</mutator></mutators>` ou grupo balanceado de operadores para análise aprofundada de robustez.

#### 2.1.2. Configuração Específica dos Módulos (`pecas-service`, `clientes-service`, `representantes-service`)
- **RF-PIT-03 - Target Classes e Target Tests**:
  - `pecas-service`:
    - `targetClasses`: `br.pucrs.construcao.pecas.service.*`, `br.pucrs.construcao.pecas.controller.*`, `br.pucrs.construcao.pecas.persistence.*`
    - `targetTests`: `br.pucrs.construcao.pecas.*Test*`
    - `excludedClasses`: `br.pucrs.construcao.pecas.PecasServiceApplication`, `br.pucrs.construcao.pecas.config.*`, `br.pucrs.construcao.pecas.dto.*`
  - `clientes-service`:
    - `targetClasses`: `br.pucrs.construcao.clientes.service.*`, `br.pucrs.construcao.clientes.controller.*`, `br.pucrs.construcao.clientes.persistence.*`
    - `targetTests`: `br.pucrs.construcao.clientes.*Test*`
    - `excludedClasses`: `br.pucrs.construcao.clientes.ClientesServiceApplication`, `br.pucrs.construcao.clientes.config.*`, `br.pucrs.construcao.clientes.dto.*`
  - `representantes-service`:
    - `targetClasses`: `br.pucrs.construcao.representantes.service.*`, `br.pucrs.construcao.representantes.controller.*`, `br.pucrs.construcao.representantes.persistence.*`
    - `targetTests`: `br.pucrs.construcao.representantes.*Test*`
    - `excludedClasses`: `br.pucrs.construcao.representantes.RepresentantesServiceApplication`, `br.pucrs.construcao.representantes.config.*`, `br.pucrs.construcao.representantes.dto.*`

#### 2.1.3. Análise e Fortalecimento dos Testes (Mutant Killing)
- **RF-PIT-04**: Executar o Pitest e identificar mutantes sobreviventes (*Surviving Mutants*).
- **RF-PIT-05**: Refinar as asserções dos testes (`AssertJ` / `Mockito`) nos casos onde mutantes sobreviveram, por exemplo:
  - Verificação de argumentos exatos passados aos métodos (`verify(repository).save(argThat(...))`).
  - Asserções estritas em valores de retorno de DTOs e entidades.
  - Validação de tratamento de exceções e limites de validação (ex: verificação do status HTTP e mensagem exata de erro).
- **RF-PIT-06**: Garantir taxa mínima de mutação (*Mutation Score*) >= 80% nas classes alvo de negócio.

#### 2.1.4. Automação e Facilidade de Execução
- **RF-PIT-07**: Fornecer scripts e perfis Maven ou comandos documentados para executar os testes de mutação individualmente por microsserviço ou no monorepo completo:
  - Comando individual: `mvn test-compile pitest:mutationCoverage -pl pecas-service`
  - Comando global: `mvn test-compile pitest:mutationCoverage`
  - Script auxiliar `run-mutation-tests.sh` para geração centralizada e relatório consolidado.

---

### 2.2. Requisitos Não-Funcionais
- **RNF-PIT-01 - Desempenho e Isolamento**: A suíte de mutação deve se beneficiar do isolamento dos testes unitários já criados (sem subir o contexto Spring completo), permitindo que centenas de mutantes sejam avaliados em menos de 1 minuto por módulo.
- **RNF-PIT-02 - Compatibilidade de Toolchain**: Compatibilidade estrita com Java 17 LTS, Maven 3.9 e JUnit 5 Jupiter.
- **RNF-PIT-03 - Não Interferência no Build Regular**: A execução regular do comando `mvn test` ou `mvn package` não deve sofrer penalidade de tempo; o ciclo do Pitest será disparado sob demanda através da meta `pitest:mutationCoverage`.

---

## 3. Architecture & Tech Stack

```
+---------------------------------------------------------------------------------------+
|                                  PITEST MUTATION PIPELINE                             |
+---------------------------------------------------------------------------------------+
                                           |
                                           v
                             +---------------------------+
                             |   Compilação Bytecode     |
                             |   (Java 17 Classes)       |
                             +---------------------------+
                                           |
                                           v
                             +---------------------------+
                             |  Injeção de Mutações      |
                             |  (Conditionals, Math,     |
                             |   Return Values, Void)    |
                             +---------------------------+
                                           |
                    +----------------------+----------------------+
                    |                                             |
                    v                                             v
        +-----------------------+                     +-----------------------+
        |  Mutante Gerado (M1)  |                     |  Mutante Gerado (M2)  |
        |  (if changed to if!)  |                     | (return true -> false)|
        +-----------------------+                     +-----------------------+
                    |                                             |
                    v                                             v
        +-----------------------+                     +-----------------------+
        |  Executa Testes       |                     |  Executa Testes       |
        |  Unitários (JUnit 5)  |                     |  Unitários (JUnit 5)  |
        +-----------------------+                     +-----------------------+
                    |                                             |
           [Teste Falhou]                                [Nenhum Teste Falhou]
                    |                                             |
                    v                                             v
        +-----------------------+                     +-----------------------+
        |     MUTANT KILLED     |                     |    MUTANT SURVIVED    |
        |       (Sucesso)       |                     |  (Alerta: Falta Teste)|
        +-----------------------+                     +-----------------------+
                                           |
                                           v
                             +---------------------------+
                             |   Relatório HTML / XML    |
                             | target/pit-reports/index  |
                             +---------------------------+
```

### 3.1. Operadores de Mutação (Mutators)
Será utilizado o conjunto `STRONGER` do Pitest, que inclui:
- **Conditionals Boundary Mutator**: altera `<`, `<=`, `>`, `>=`.
- **Increments Mutator**: altera `++` para `--` e vice-versa.
- **Invert Negatives Mutator**: inverte números negativos e positivos.
- **Math Mutator**: altera operadores binários (`+`, `-`, `*`, `/`, `%`).
- **Negate Conditionals Mutator**: inverte comparações condicionais (`==` para `!=`, `!=` para `==`).
- **Return Values Mutator**: substitui valores de retorno de métodos por `null`, zero, strings vazias ou falsos.
- **Void Method Calls Mutator**: remove chamadas a métodos `void` (ex: `repository.delete()`, chamadas a contadores).

### 3.2. Estrutura de Artefatos Gerados
Após a execução, cada microsserviço terá seus relatórios disponíveis em:
- `app_build/pecas-service/target/pit-reports/index.html`
- `app_build/clientes-service/target/pit-reports/index.html`
- `app_build/representantes-service/target/pit-reports/index.html`

---

## 4. Plano de Execução do Ciclo Autônomo

1. **Fase 1 (Engenharia - @engineer)**:
   - Configurar o `pom.xml` pai (`app_build/pom.xml`) com o plugin do Pitest e o JUnit 5 plugin.
   - Adicionar as configurações específicas de `targetClasses`, `targetTests` e `excludedClasses` nos `pom.xml` de `pecas-service`, `clientes-service` e `representantes-service`.
   - Adicionar o script `app_build/run-mutation-tests.sh` para facilitar a execução e abertura dos relatórios.
2. **Fase 2 (Qualidade & Auditoria - @qa)**:
   - Executar os testes de mutação com Pitest em todos os módulos.
   - Analisar o relatório de mutantes gerado.
   - Identificar eventuais mutantes sobreviventes nos serviços e controladores.
   - Reforçar e adicionar asserções nos testes existentes para eliminar mutantes remanescentes e atingir alto score de mutação (> 80%).
   - Reexecutar o Pitest para validar a morte dos mutantes.
3. **Fase 3 (DevOps - @devops)**:
   - Fornecer instruções de execução via linha de comando e disponibilizar visualização dos relatórios gerados.

---

## 5. State Management & Test Isolation
- **Processos Isolados**: Cada lote de mutações é executado em processos forkeados pelo Pitest, garantindo isolamento total de memória e evitando efeitos colaterais entre mutantes.
- **Execução em Memória**: Como os testes de serviços utilizam repositórios fakes em memória (`InMemoryPecaRepository`, etc.) e mocks do Mockito, não há latência de I/O ou banco de dados externo, o que resulta em execução ultra rápida da suíte de mutação.
