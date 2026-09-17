package br.pucrs.construcao.clientes.controller;

import br.pucrs.construcao.clientes.dto.ClienteRequestDTO;
import br.pucrs.construcao.clientes.dto.ClienteResponseDTO;
import br.pucrs.construcao.clientes.service.ClienteService;
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
@DisplayName("Testes Unitários de ClienteController - Isolando o Framework Web")
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private MockMvc mockMvcStandalone;

    @BeforeEach
    void setUp() {
        mockMvcStandalone = MockMvcBuilders.standaloneSetup(clienteController).build();
    }

    @Nested
    @DisplayName("1. Testes Unitários Puros em POJO (Isolamento Total de Web Framework e Container)")
    class PurePojoTests {

        @Test
        @DisplayName("cadastrar: deve delegar para clienteService e retornar ResponseEntity 201 CREATED")
        void cadastrar_DeveRetornarCreated() {
            // Arrange
            ClienteRequestDTO request = new ClienteRequestDTO("123.456.789-00", "Mariana Rios");
            ClienteResponseDTO response = new ClienteResponseDTO(1L, "123.456.789-00", "Mariana Rios");
            when(clienteService.cadastrar(request)).thenReturn(response);

            // Act - Chamada direta ao método POJO sem framework web
            ResponseEntity<ClienteResponseDTO> resultado = clienteController.cadastrar(request);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resultado.getBody()).isSameAs(response);
            verify(clienteService, times(1)).cadastrar(request);
        }

        @Test
        @DisplayName("listarTodos: deve delegar para clienteService e retornar ResponseEntity 200 OK")
        void listarTodos_DeveRetornarOk() {
            // Arrange
            List<ClienteResponseDTO> lista = List.of(
                    new ClienteResponseDTO(1L, "111.111.111-11", "Lucas Mendes"),
                    new ClienteResponseDTO(2L, "222.222.222-22", "Fernanda Lima")
            );
            when(clienteService.listarTodos()).thenReturn(lista);

            // Act
            ResponseEntity<List<ClienteResponseDTO>> resultado = clienteController.listarTodos();

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).hasSize(2);
            assertThat(resultado.getBody()).isSameAs(lista);
            verify(clienteService, times(1)).listarTodos();
        }

        @Test
        @DisplayName("buscarPorId: deve delegar para clienteService e retornar ResponseEntity 200 OK")
        void buscarPorId_DeveRetornarOk() {
            // Arrange
            ClienteResponseDTO response = new ClienteResponseDTO(5L, "333.333.333-33", "Gabriel Rocha");
            when(clienteService.buscarPorId(5L)).thenReturn(response);

            // Act
            ResponseEntity<ClienteResponseDTO> resultado = clienteController.buscarPorId(5L);

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(clienteService, times(1)).buscarPorId(5L);
        }

        @Test
        @DisplayName("buscarPorCpf: deve delegar para clienteService e retornar ResponseEntity 200 OK")
        void buscarPorCpf_DeveRetornarOk() {
            // Arrange
            ClienteResponseDTO response = new ClienteResponseDTO(8L, "444.444.444-44", "Beatriz Souza");
            when(clienteService.buscarPorCpf("444.444.444-44")).thenReturn(response);

            // Act
            ResponseEntity<ClienteResponseDTO> resultado = clienteController.buscarPorCpf("444.444.444-44");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(response);
            verify(clienteService, times(1)).buscarPorCpf("444.444.444-44");
        }

        @Test
        @DisplayName("buscarPorNome: deve delegar para clienteService e retornar ResponseEntity 200 OK")
        void buscarPorNome_DeveRetornarOk() {
            // Arrange
            List<ClienteResponseDTO> lista = List.of(new ClienteResponseDTO(1L, "555.555.555-55", "Beatriz Souza"));
            when(clienteService.buscarPorNome("beatriz")).thenReturn(lista);

            // Act
            ResponseEntity<List<ClienteResponseDTO>> resultado = clienteController.buscarPorNome("beatriz");

            // Assert
            assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resultado.getBody()).isEqualTo(lista);
            verify(clienteService, times(1)).buscarPorNome("beatriz");
        }
    }

    @Nested
    @DisplayName("2. Testes Standalone MockMvc (Isolamento de Servidor HTTP)")
    class StandaloneMockMvcTests {

        @Test
        @DisplayName("POST /clientes: deve retornar 201 CREATED")
        void postClientes_RetornaCreated() throws Exception {
            ClienteResponseDTO response = new ClienteResponseDTO(1L, "123.456.789-00", "Mariana Rios");
            when(clienteService.cadastrar(any(ClienteRequestDTO.class))).thenReturn(response);

            String jsonPayload = """
                    {
                        "cpf": "123.456.789-00",
                        "nome": "Mariana Rios"
                    }
                    """;

            mockMvcStandalone.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.cpf").value("123.456.789-00"))
                    .andExpect(jsonPath("$.nome").value("Mariana Rios"));
        }

        @Test
        @DisplayName("GET /clientes/{id}: deve retornar 200 OK")
        void getClientePorId_RetornaOk() throws Exception {
            ClienteResponseDTO response = new ClienteResponseDTO(2L, "111.111.111-11", "Lucas");
            when(clienteService.buscarPorId(2L)).thenReturn(response);

            mockMvcStandalone.perform(get("/clientes/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.nome").value("Lucas"));
        }
    }
}
