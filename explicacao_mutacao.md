# Ciclo 3 — Testes de Mutação com PITest: o que foi feito, por quê e como usar

Este documento descreve detalhadamente a implementação dos **Testes de Mutação com a biblioteca PITest** no ecossistema de microsserviços do projeto, as motivações de cada decisão técnica, a análise dos mutantes identificados e a forma de execução e visualização dos relatórios.

---

## 1. Visão Geral do Ciclo

| Categoria | O que foi feito |
| :--- | :--- |
| **Integração PITest** | Configuração do `pitest-maven` (1.15.8) com `pitest-junit5-plugin` (1.2.1) no POM pai e nos 3 microsserviços de negócio |
| **Alvos de Mutação** | Camadas de serviço de negócio (`*.service.*`) avaliadas pelas suítes unitárias puras (`*ServiceTest`) |
| **Auditoria e Refinamento de Testes** | Identificação e eliminação de mutantes sobreviventes nas regras de negócio (buscas parciais, métricas 404 e gauges de repositório) |
| **Relatórios Determinísticos** | Geração automática de relatórios visuais HTML e XML em `target/pit-reports/index.html` (sem timestamp no caminho) |
| **Automação de Execução** | Criação de scripts executores para Windows (`run-mutation-tests.bat`) e Linux/macOS/Bash (`run-mutation-tests.sh`) |
| **Quality Gate** | Limite mínimo estipulado de **80%** de Mutation Score; resultado atingido: **100% de mutantes mortos em todos os serviços** |

---

## 2. Por que Testes de Mutação?

A cobertura tradicional de código (line coverage e branch coverage) apenas mede se determinadas linhas de código foram executadas durante a suíte de testes. No entanto:
> **Linha executada não significa comportamento verificado.**

Um teste pode passar por 100% das linhas sem possuir asserções suficientes (`assert`) ou sem validar o retorno real de uma operação. O teste de mutação insere deliberadamente pequenos defeitos sintáticos ("mutantes") no bytecode da aplicação compilada:
- Se a suíte de testes **falhar**, o mutante é considerado **MORTO (Killed)** -> indica que a suíte é sensível a falhas naquele ponto.
- Se a suíte de testes **continuar passando**, o mutante **SOBREVIVEU (Survived)** -> revela uma fraqueza no teste ou código inútil/morto.

---

## 3. Configurações Maven Implementadas

### 3.1. POM Raiz (`app_build/pom.xml`)
No gerenciamento centralizado de plugins (`<pluginManagement>`), foram definidos os parâmetros padrão compartilhados:
- **`pitest.version`**: `1.15.8`
- **`pitest-junit5.version`**: `1.2.1`
- **`outputFormats`**: `HTML`, `XML`
- **`timestampedReports`**: `false` (gera sempre em `target/pit-reports/index.html`, facilitando links e automações)
- **`threads`**: `4` (execução paralela de mutantes para máxima performance)
- **`jvmArgs`**: `-XX:+EnableDynamicAgentLoading` (garante compatibilidade plena com Java 17 e Java 21)

### 3.2. Microsserviços de Domínio (`pecas-service`, `clientes-service`, `representantes-service`)
Em cada `pom.xml`, o plugin foi ativado com o escopo delimitado:
```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <configuration>
        <targetClasses>
            <param>br.pucrs.construcao.<servico>.service.*</param>
        </targetClasses>
        <targetTests>
            <param>br.pucrs.construcao.<servico>.service.*Test</param>
        </targetTests>
        <mutationThreshold>80</mutationThreshold>
    </configuration>
</plugin>
```

---

## 4. Auditoria de Mutantes e Refinamento dos Testes (O que a QA encontrou)

Durante a primeira execução do PITest no `pecas-service`, a cobertura de linha já era de 100%, mas a **cobertura de mutação foi de apenas 77%**, reprovando o threshold configurado de 80%.

### Mutantes que haviam sobrevivido:
1. **`EmptyObjectReturnValsMutator` em `buscarPorNome`**:
   - *O que o PITest fez*: Substituiu o retorno de `buscarPorNome` por `Collections.emptyList()`.
   - *Por que sobreviveu*: O teste existente apenas testava a busca que retornava lista vazia! Quando a busca realmente encontrava itens, não havia asserção para verificar se os dados eram retornados e mapeados no DTO.
   - *Como foi morto*: Foi adicionado o teste `buscarPorNomeDeveRetornarPecasEncontradas()` que valida que uma lista com itens é retornada e corretamente mapeada para DTO.

2. **`VoidMethodCallMutator` em `buscarPorNumeroIdentificacao` / `buscarPorCpf`**:
   - *O que o PITest fez*: Removeu a chamada `consultaNaoEncontradaCounter.increment()`.
   - *Por que sobreviveu*: O teste capturava a exceção 404 (`ResponseStatusException`), mas não verificava se o contador de métricas do Micrometer havia sido incrementado.
   - *Como foi morto*: Adicionada a asserção `assertThat(contador("pecas.consulta.nao_encontrada", null)).isEqualTo(1.0);`.

3. **`PrimitiveReturnsMutator` no registro do Gauge (`r -> r.count()`)**:
   - *O que o PITest fez*: Substituiu o retorno do contador do gauge por `0.0d`.
   - *Por que sobreviveu*: Nenhum teste unitário consultava o valor do gauge registrado no `SimpleMeterRegistry`.
   - *Como foi morto*: Adicionado o teste `gaugeDeveRefletirContagemDoRepositorio()` validando que o gauge reflete o número de registros do repositório.

As mesmas correções foram aplicadas preventivamente em `clientes-service` e `representantes-service`.

---

## 5. Resultados Finais por Microsserviço

Após o refinamento dos testes pelo Engenheiro e QA:

| Módulo | Classes Testadas | Mutantes Gerados | Mutantes Mortos | Mutation Score | Test Strength | Line Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`pecas-service`** | `PecaService` | 13 | 13 | **100%** | **100%** | **100%** |
| **`clientes-service`** | `ClienteService` | 13 | 13 | **100%** | **100%** | **100%** |
| **`representantes-service`**| `RepresentanteService` | 13 | 13 | **100%** | **100%** | **100%** |

---

## 6. Como Executar os Testes de Mutação

### Opção 1: Via Scripts Facilitadores (Recomendado)
Na pasta `app_build`:
- **Windows**:
  ```cmd
  cd app_build
  run-mutation-tests.bat
  ```
  *(Ou para um serviço específico: `run-mutation-tests.bat pecas-service`)*

- **Linux / macOS / Bash**:
  ```bash
  cd app_build
  ./run-mutation-tests.sh
  ```

### Opção 2: Via Maven Diretamente
```bash
# Para pecas-service:
cd app_build/pecas-service
mvn test-compile pitest:mutationCoverage

# Para clientes-service:
cd app_build/clientes-service
mvn test-compile pitest:mutationCoverage

# Para representantes-service:
cd app_build/representantes-service
mvn test-compile pitest:mutationCoverage
```

---

## 7. Como Visualizar os Relatórios HTML

Abra diretamente no navegador qualquer um dos seguintes arquivos:
- `app_build/pecas-service/target/pit-reports/index.html`
- `app_build/clientes-service/target/pit-reports/index.html`
- `app_build/representantes-service/target/pit-reports/index.html`

O relatório apresenta:
- Tabela resumo com **Line Coverage**, **Mutation Coverage** e **Test Strength**.
- Visualização do código fonte anotado com cores (verde para linhas e mutantes mortos, vermelho se houvesse sobreviventes).
- Lista detalhada de cada mutador aplicado por número de linha e qual teste unitário foi responsável por matar o mutante.
