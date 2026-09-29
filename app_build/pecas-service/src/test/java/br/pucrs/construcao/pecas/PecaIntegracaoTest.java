package br.pucrs.construcao.pecas;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.repository.PecaRepository;
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
@DisplayName("Pecas - fluxo de integracao ponta a ponta")
class PecaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PecaRepository pecaRepository;

    @BeforeEach
    void limparBase() {
        pecaRepository.deleteAll();
    }

    @Test
    @DisplayName("cadastrar e consultar a peca pelo numero percorrendo todas as camadas")
    void deveCadastrarEConsultarPeca() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new PecaRequestDTO("PN-001", "Parafuso M8", "Sextavado"));

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/pecas/numero/PN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Parafuso M8"));

        mockMvc.perform(get("/pecas/busca").param("nome", "paraf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("cadastrar o mesmo numero de identificacao duas vezes deve resultar em 409")
    void deveRejeitarNumeroDuplicado() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new PecaRequestDTO("PN-001", "Parafuso M8", "Sextavado"));

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
