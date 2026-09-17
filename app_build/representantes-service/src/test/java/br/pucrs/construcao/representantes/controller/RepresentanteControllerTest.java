package br.pucrs.construcao.representantes.controller;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.service.RepresentanteService;
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
@DisplayName("Testes Unitários de RepresentanteController - Isolando o Framework Web")
class RepresentanteControllerTest {

    @Mock
    private RepresentanteService representanteService;

    @InjectMocks
    private RepresentanteController representanteController;

    private MockMvc mockMvcStandalone;

    @BeforeEach
    void setUp() {
        mockMvcStandalone = MockMvcBuilders.standaloneSetup(representanteController).build();
    }

    @Nested
    @DisplayName("1. Testes Unitários Puros em POJO (Isolamento Total de Framework Web)")
    class PurePojoTests {

        @Test
        @DisplayName("cadastrar: deve delegar para representanteService e retornar ResponseEntity 201 CREATED")
        void cadastrar_DeveRetornarCreated() {
            // Arrange
            RepresentanteRequestDTO request = new RepresentanteRequestDTO("123.456.789-00", "Paula Fernandes");
            RepresentanteResponseDTO response = new RepresentanteResponseDTO(1L, "123.456.789-00", "Paula Fernandes");
            when(representanteService.cadastrar(request)).thenReturn(response);

            // Act - Chamada direta ao método Java, sem contexto servlet
            ResponseEntity<RepresentanteResponseDTO> resultado = representanteController.cadastrar(request);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resultado.getBody()).isSameAs(response);
            verify(representanteService, times(1)).cadastrar(request);
        }

        @Test
        @DisplayName("listarTodos: deve delegar para representanteService e retornar ResponseEntity 200 OK")
        void listarTodos_DeveRetornarOk() {
            // Arrange
            List<RepresentanteResponseDTO> lista = List.of(
                    new RepresentanteResponseDTO(1L, "111.111.111-11", "Juliana Lima"),
                    new RepresentanteResponseDTO(2L, "222.222.222-22", "Marcos Paulo")
            );
            when(representanteService.listarTodos()).thenReturn(lista);

            // Act
            ResponseEntity<List<RepresentanteResponseDTO>> resultado = representanteController.listarTodos();

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).hasSize(2);
            assertThat(resultado.getBody()).isSameAs(lista);
            verify(representanteService, times(1)).listarTodos();
        }

        @Test
        @DisplayName("buscarPorId: deve delegar para representanteService e retornar ResponseEntity 200 OK")
        void buscarPorId_DeveRetornarOk() {
            // Arrange
            RepresentanteResponseDTO response = new RepresentanteResponseDTO(3L, "333.333.333-33", "Thiago Silva");
            when(representanteService.buscarPorId(3L)).thenReturn(response);

            // Act
            ResponseEntity<RepresentanteResponseDTO> resultado = representanteController.buscarPorId(3L);

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(representanteService, times(1)).buscarPorId(3L);
        }

        @Test
        @DisplayName("buscarPorCpf: deve delegar para representanteService e retornar ResponseEntity 200 OK")
        void buscarPorCpf_DeveRetornarOk() {
            // Arrange
            RepresentanteResponseDTO response = new RepresentanteResponseDTO(4L, "444.444.444-44", "Fernanda Santos");
            when(representanteService.buscarPorCpf("444.444.444-44")).thenReturn(response);

            // Act
            ResponseEntity<RepresentanteResponseDTO> resultado = representanteController.buscarPorCpf("444.444.444-44");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(representanteService, times(1)).buscarPorCpf("444.444.444-44");
        }

        @Test
        @DisplayName("buscarPorNome: deve delegar para representanteService e retornar ResponseEntity 200 OK")
        void buscarPorNome_DeveRetornarOk() {
            // Arrange
            List<RepresentanteResponseDTO> lista = List.of(new RepresentanteResponseDTO(1L, "555.555.555-55", "Fernanda Santos"));
            when(representanteService.buscarPorNome("fernanda")).thenReturn(lista);

            // Act
            ResponseEntity<List<RepresentanteResponseDTO>> resultado = representanteController.buscarPorNome("fernanda");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(lista);
            verify(representanteService, times(1)).buscarPorNome("fernanda");
        }
    }

    @Nested
    @DisplayName("2. Testes Standalone MockMvc (Isolamento de Servidor HTTP)")
    class StandaloneMockMvcTests {

        @Test
        @DisplayName("POST /representantes: deve retornar 201 CREATED")
        void postRepresentantes_RetornaCreated() throws Exception {
            RepresentanteResponseDTO response = new RepresentanteResponseDTO(1L, "123.456.789-00", "Paula Fernandes");
            when(representanteService.cadastrar(any(RepresentanteRequestDTO.class))).thenReturn(response);

            String jsonPayload = """
                    {
                        "cpf": "123.456.789-00",
                        "nome": "Paula Fernandes"
                    }
                    """;

            mockMvcStandalone.perform(post("/representantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.cpf").value("123.456.789-00"))
                    .andExpect(jsonPath("$.nome").value("Paula Fernandes"));
        }

        @Test
        @DisplayName("GET /representantes/{id}: deve retornar 200 OK")
        void getRepresentantePorId_RetornaOk() throws Exception {
            RepresentanteResponseDTO response = new RepresentanteResponseDTO(2L, "111.111.111-11", "Juliana");
            when(representanteService.buscarPorId(2L)).thenReturn(response);

            mockMvcStandalone.perform(get("/representantes/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.nome").value("Juliana"));
        }
    }
}
