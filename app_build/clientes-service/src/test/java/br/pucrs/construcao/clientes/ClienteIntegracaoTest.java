package br.pucrs.construcao.clientes;

import br.pucrs.construcao.clientes.dto.ClienteRequestDTO;
import br.pucrs.construcao.clientes.repository.ClienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao: contexto completo (web + servico + JPA + H2), validando
 * o fluxo ponta a ponta que os testes unitarios cobrem apenas em partes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Clientes - fluxo de integracao ponta a ponta")
class ClienteIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @BeforeEach
    void limparBase() {
        clienteRepository.deleteAll();
    }

    @Test
    @DisplayName("cadastrar e consultar o cliente pelo CPF percorrendo todas as camadas")
    void deveCadastrarEConsultarCliente() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new ClienteRequestDTO("12345678901", "Maria Silva"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/clientes/cpf/12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        mockMvc.perform(get("/clientes/busca").param("nome", "mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("cadastrar o mesmo CPF duas vezes deve resultar em 409")
    void deveRejeitarCpfDuplicado() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new ClienteRequestDTO("12345678901", "Maria Silva"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
