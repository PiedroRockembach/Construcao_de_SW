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

### Execução dos Testes:
```bash
cd app_build
mvn test
```