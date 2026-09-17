package br.pucrs.construcao.pecas.controller;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.service.PecaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários de PecaController - Isolando o Framework Web")
class PecaControllerTest {

    @Mock
    private PecaService pecaService;

    @InjectMocks
    private PecaController pecaController;

    private MockMvc mockMvcStandalone;

    @BeforeEach
    void setUp() {
        // Setup Standalone do MockMvc: isola completamente Tomcat, servlets e ApplicationContext do Spring
        mockMvcStandalone = MockMvcBuilders.standaloneSetup(pecaController).build();
    }

    @Nested
    @DisplayName("1. Testes Unitários Puros em POJO (Zero dependência de Servlet / Web Context)")
    class PurePojoTests {

        @Test
        @DisplayName("cadastrar: deve delegar para pecaService e retornar ResponseEntity 201 CREATED com corpo")
        void cadastrar_DeveRetornarCreated() {
            // Arrange
            PecaRequestDTO request = new PecaRequestDTO("PEC-001", "Disco de Freio", "Disco ventilado");
            PecaResponseDTO response = new PecaResponseDTO(1L, "PEC-001", "Disco de Freio", "Disco ventilado");
            when(pecaService.cadastrar(request)).thenReturn(response);

            // Act - Chamada direta ao método Java, sem passar por HTTP/Web
            ResponseEntity<PecaResponseDTO> resultado = pecaController.cadastrar(request);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resultado.getBody()).isSameAs(response);
            verify(pecaService, times(1)).cadastrar(request);
        }

        @Test
        @DisplayName("listarTodas: deve delegar para pecaService e retornar ResponseEntity 200 OK")
        void listarTodas_DeveRetornarOk() {
            // Arrange
            List<PecaResponseDTO> lista = List.of(
                    new PecaResponseDTO(1L, "PEC-001", "Vela de Ignição", "Vela"),
                    new PecaResponseDTO(2L, "PEC-002", "Cabo de Vela", "Cabo")
            );
            when(pecaService.listarTodas()).thenReturn(lista);

            // Act
            ResponseEntity<List<PecaResponseDTO>> resultado = pecaController.listarTodas();

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).hasSize(2);
            assertThat(resultado.getBody()).isSameAs(lista);
            verify(pecaService, times(1)).listarTodas();
        }

        @Test
        @DisplayName("buscarPorId: deve delegar para pecaService e retornar ResponseEntity 200 OK")
        void buscarPorId_DeveRetornarOk() {
            // Arrange
            PecaResponseDTO response = new PecaResponseDTO(5L, "PEC-005", "Radiador", "Radiador");
            when(pecaService.buscarPorId(5L)).thenReturn(response);

            // Act
            ResponseEntity<PecaResponseDTO> resultado = pecaController.buscarPorId(5L);

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(pecaService, times(1)).buscarPorId(5L);
        }

        @Test
        @DisplayName("buscarPorNome: deve delegar para pecaService e retornar ResponseEntity 200 OK")
        void buscarPorNome_DeveRetornarOk() {
            // Arrange
            List<PecaResponseDTO> lista = List.of(new PecaResponseDTO(1L, "PEC-001", "Radiador", "Radiador"));
            when(pecaService.buscarPorNome("radiador")).thenReturn(lista);

            // Act
            ResponseEntity<List<PecaResponseDTO>> resultado = pecaController.buscarPorNome("radiador");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(lista);
            verify(pecaService, times(1)).buscarPorNome("radiador");
        }

        @Test
        @DisplayName("buscarPorNumero: deve delegar para pecaService e retornar ResponseEntity 200 OK")
        void buscarPorNumero_DeveRetornarOk() {
            // Arrange
            PecaResponseDTO response = new PecaResponseDTO(10L, "PEC-999", "Bateria", "60Ah");
            when(pecaService.buscarPorNumeroIdentificacao("PEC-999")).thenReturn(response);

            // Act
            ResponseEntity<PecaResponseDTO> resultado = pecaController.buscarPorNumero("PEC-999");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(pecaService, times(1)).buscarPorNumeroIdentificacao("PEC-999");
        }
    }

    @Nested
    @DisplayName("2. Testes Standalone MockMvc (Isolando Container de Servlet e Aplicação)")
    class StandaloneMockMvcTests {

        @Test
        @DisplayName("POST /pecas: deve retornar 201 CREATED e JSON correto")
        void postPecas_RetornaCreated() throws Exception {
            PecaResponseDTO response = new PecaResponseDTO(1L, "PEC-001", "Amortecedor", "Descricao");
            when(pecaService.cadastrar(any(PecaRequestDTO.class))).thenReturn(response);

            String jsonPayload = """
                    {
                        "numeroIdentificacao": "PEC-001",
                        "nome": "Amortecedor",
                        "descricao": "Descricao"
                    }
                    """;

            mockMvcStandalone.perform(post("/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.numeroIdentificacao").value("PEC-001"))
                    .andExpect(jsonPath("$.nome").value("Amortecedor"));
        }

        @Test
        @DisplayName("GET /pecas/{id}: deve retornar 200 OK e JSON")
        void getPecaPorId_RetornaOk() throws Exception {
            PecaResponseDTO response = new PecaResponseDTO(7L, "PEC-007", "Farol LED", "Farol");
            when(pecaService.buscarPorId(7L)).thenReturn(response);

            mockMvcStandalone.perform(get("/pecas/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.nome").value("Farol LED"));
        }
    }
}
