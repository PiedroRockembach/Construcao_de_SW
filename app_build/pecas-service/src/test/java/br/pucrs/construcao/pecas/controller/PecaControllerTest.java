package br.pucrs.construcao.pecas.controller;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.service.PecaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste do controller isolando o framework web: sobe apenas a fatia MVC
 * (@WebMvcTest) e substitui o servico por um @MockBean, de modo que nenhuma
 * regra de negocio, repositorio ou banco participa do teste.
 */
@WebMvcTest(PecaController.class)
@DisplayName("PecaController - contrato HTTP isolado do servico")
class PecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PecaService pecaService;

    @Test
    @DisplayName("POST /pecas deve responder 201 com o corpo da peca criada")
    void cadastrarDeveRetornarCreated() throws Exception {
        when(pecaService.cadastrar(any(PecaRequestDTO.class)))
                .thenReturn(new PecaResponseDTO(1L, "PN-001", "Parafuso M8", "Sextavado"));

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequestDTO("PN-001", "Parafuso M8", "Sextavado"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroIdentificacao").value("PN-001"))
                .andExpect(jsonPath("$.nome").value("Parafuso M8"));
    }

    @Test
    @DisplayName("POST /pecas deve responder 400 e nao chamar o servico quando faltam campos obrigatorios")
    void cadastrarDeveValidarPayload() throws Exception {
        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequestDTO("", "", null))))
                .andExpect(status().isBadRequest());

        verify(pecaService, never()).cadastrar(any(PecaRequestDTO.class));
    }

    @Test
    @DisplayName("POST /pecas deve propagar 409 lancado pelo servico")
    void cadastrarDevePropagarConflito() throws Exception {
        when(pecaService.cadastrar(any(PecaRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "numero duplicado"));

        mockMvc.perform(post("/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequestDTO("PN-001", "Parafuso M8", null))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /pecas deve responder 200 com a lista")
    void listarTodasDeveRetornarOk() throws Exception {
        when(pecaService.listarTodas()).thenReturn(List.of(
                new PecaResponseDTO(1L, "PN-001", "Parafuso M8", "Sextavado"),
                new PecaResponseDTO(2L, "PN-002", "Porca M8", "Galvanizada")));

        mockMvc.perform(get("/pecas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].nome").value("Porca M8"));
    }

    @Test
    @DisplayName("GET /pecas/{id} deve responder 404 quando o servico nao encontra")
    void buscarPorIdDeveRetornarNotFound() throws Exception {
        when(pecaService.buscarPorId(anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "nao encontrada"));

        mockMvc.perform(get("/pecas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /pecas/numero/{numeroIdentificacao} deve repassar o path variable ao servico")
    void buscarPorNumeroDeveRepassarParametro() throws Exception {
        when(pecaService.buscarPorNumeroIdentificacao("PN-001"))
                .thenReturn(new PecaResponseDTO(1L, "PN-001", "Parafuso M8", "Sextavado"));

        mockMvc.perform(get("/pecas/numero/PN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Parafuso M8"));

        verify(pecaService).buscarPorNumeroIdentificacao("PN-001");
    }

    @Test
    @DisplayName("GET /pecas/busca deve usar string vazia quando o parametro nome e omitido")
    void buscarPorNomeDeveUsarValorPadrao() throws Exception {
        when(pecaService.buscarPorNome("")).thenReturn(List.of());

        mockMvc.perform(get("/pecas/busca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(pecaService).buscarPorNome("");
    }
}
