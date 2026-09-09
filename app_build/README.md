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
