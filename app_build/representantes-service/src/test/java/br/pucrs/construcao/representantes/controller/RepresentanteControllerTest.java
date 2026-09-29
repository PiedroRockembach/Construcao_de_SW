package br.pucrs.construcao.representantes.controller;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.service.RepresentanteService;
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
import static org.mockito.ArgumentMatchers.eq;
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
@WebMvcTest(RepresentanteController.class)
@DisplayName("RepresentanteController - contrato HTTP isolado do servico")
class RepresentanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepresentanteService representanteService;

    @Test
    @DisplayName("POST /representantes deve responder 201 com o corpo do representante criado")
    void cadastrarDeveRetornarCreated() throws Exception {
        when(representanteService.cadastrar(any(RepresentanteRequestDTO.class)))
                .thenReturn(new RepresentanteResponseDTO(1L, "12345678901", "Maria Silva"));

        mockMvc.perform(post("/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepresentanteRequestDTO("12345678901", "Maria Silva"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    @DisplayName("POST /representantes deve responder 400 e nao chamar o servico quando o CPF e invalido")
    void cadastrarDeveValidarPayload() throws Exception {
        mockMvc.perform(post("/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepresentanteRequestDTO("abc", "M"))))
                .andExpect(status().isBadRequest());

        verify(representanteService, never()).cadastrar(any(RepresentanteRequestDTO.class));
    }

    @Test
    @DisplayName("POST /representantes deve propagar 409 lancado pelo servico")
    void cadastrarDevePropagarConflito() throws Exception {
        when(representanteService.cadastrar(any(RepresentanteRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "CPF duplicado"));

        mockMvc.perform(post("/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RepresentanteRequestDTO("12345678901", "Maria Silva"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /representantes deve responder 200 com a lista")
    void listarTodosDeveRetornarOk() throws Exception {
        when(representanteService.listarTodos()).thenReturn(List.of(
                new RepresentanteResponseDTO(1L, "12345678901", "Maria Silva"),
                new RepresentanteResponseDTO(2L, "10987654321", "Joao Souza")));

        mockMvc.perform(get("/representantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].nome").value("Joao Souza"));
    }

    @Test
    @DisplayName("GET /representantes/{id} deve responder 404 quando o servico nao encontra")
    void buscarPorIdDeveRetornarNotFound() throws Exception {
        when(representanteService.buscarPorId(anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "nao encontrado"));

        mockMvc.perform(get("/representantes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /representantes/cpf/{cpf} deve repassar o path variable ao servico")
    void buscarPorCpfDeveRepassarParametro() throws Exception {
        when(representanteService.buscarPorCpf("12345678901"))
                .thenReturn(new RepresentanteResponseDTO(1L, "12345678901", "Maria Silva"));

        mockMvc.perform(get("/representantes/cpf/12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        verify(representanteService).buscarPorCpf("12345678901");
    }

    @Test
    @DisplayName("GET /representantes/busca deve usar string vazia quando o parametro nome e omitido")
    void buscarPorNomeDeveUsarValorPadrao() throws Exception {
        when(representanteService.buscarPorNome(eq(""))).thenReturn(List.of());

        mockMvc.perform(get("/representantes/busca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(representanteService).buscarPorNome("");
    }
}
