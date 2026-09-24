# Relatório de Produção: Testes de Mutação com Pitest

## 1. Resumo Executivo
Os testes de mutação com o framework **Pitest (PIT)** foram integrados e validados com sucesso no ecossistema de microsserviços Java com Spring Boot 3 e JUnit 5.
Foram gerados e avaliados **45 mutantes** no total abrangendo as camadas de serviço (`*Service`) e controlador (`*Controller`), obtendo-se uma taxa de eliminação de mutantes de **100%** e **0 mutantes sobreviventes**.

---

## 2. Estatísticas Gerais por Módulo

| Módulo | Classes Alvo | Mutantes Gerados | Mutantes Eliminados | Mutantes Sobreviventes | Mutation Coverage | Test Strength |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **`pecas-service`** | `PecaService`, `PecaController` | 15 | 15 | 0 | **100%** | **100%** |
| **`clientes-service`** | `ClienteService`, `ClienteController` | 15 | 15 | 0 | **100%** | **100%** |
| **`representantes-service`** | `RepresentanteService`, `RepresentanteController` | 15 | 15 | 0 | **100%** | **100%** |
| **TOTAL CONSOLIDADO** | **6 classes** | **45** | **45** | **0** | **100%** | **100%** |

---

## 3. Artefatos e Relatórios Gerados

1. **Dashboard Unificado**:
   - `app_build/mutation-dashboard.html`
2. **Relatórios HTML Detalhados do Pitest**:
   - `app_build/pecas-service/target/pit-reports/index.html`
   - `app_build/clientes-service/target/pit-reports/index.html`
   - `app_build/representantes-service/target/pit-reports/index.html`
3. **Script de Automação**:
   - `app_build/run-mutation-tests.sh`
