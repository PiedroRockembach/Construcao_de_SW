package br.pucrs.construcao.clientes.service;

import br.pucrs.construcao.clientes.dto.ClienteRequestDTO;
import br.pucrs.construcao.clientes.dto.ClienteResponseDTO;
import br.pucrs.construcao.clientes.model.Cliente;
import br.pucrs.construcao.clientes.repository.ClienteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários de ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    private MeterRegistry meterRegistry;
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        clienteService = new ClienteService(clienteRepository, meterRegistry);
    }

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso quando CPF não existir")
    void cadastrar_Sucesso() {
        // Arrange
        ClienteRequestDTO dto = new ClienteRequestDTO("123.456.789-00", "Carlos Silva");
        when(clienteRepository.existsByCpf("123.456.789-00")).thenReturn(false);

        Cliente clienteSalvo = new Cliente("123.456.789-00", "Carlos Silva");
        clienteSalvo.setId(1L);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

        // Act
        ClienteResponseDTO resultado = clienteService.cadastrar(dto);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCpf()).isEqualTo("123.456.789-00");
        assertThat(resultado.getNome()).isEqualTo("Carlos Silva");

        // Verifica métrica
        double count = meterRegistry.get("clientes.created.total").counter().count();
        assertThat(count).isEqualTo(1.0);

        verify(clienteRepository, times(1)).existsByCpf("123.456.789-00");
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 ao cadastrar cliente com CPF duplicado")
    void cadastrar_ConflitoCpf() {
        // Arrange
        ClienteRequestDTO dto = new ClienteRequestDTO("123.456.789-00", "Carlos Silva");
        when(clienteRepository.existsByCpf("123.456.789-00")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.cadastrar(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("Já existe um cliente cadastrado com o CPF");
                });

        verify(clienteRepository, times(1)).existsByCpf("123.456.789-00");
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve listar todos os clientes cadastrados")
    void listarTodos_Sucesso() {
        // Arrange
        Cliente c1 = new Cliente("111.111.111-11", "Ana Costa");
        c1.setId(1L);
        Cliente c2 = new Cliente("222.222.222-22", "Bruno Dias");
        c2.setId(2L);

        when(clienteRepository.findAll()).thenReturn(List.of(c1, c2));

        // Act
        List<ClienteResponseDTO> resultado = clienteService.listarTodos();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNome()).isEqualTo("Ana Costa");
        assertThat(resultado.get(1).getNome()).isEqualTo("Bruno Dias");
        verify(clienteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void buscarPorId_Sucesso() {
        // Arrange
        Cliente cliente = new Cliente("123.456.789-00", "Carlos Silva");
        cliente.setId(10L);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        // Act
        ClienteResponseDTO resultado = clienteService.buscarPorId(10L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getNome()).isEqualTo("Carlos Silva");
        verify(clienteRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por ID inexistente")
    void buscarPorId_NaoEncontrado() {
        // Arrange
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Cliente não encontrado com o ID: 99");
                });

        verify(clienteRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve buscar cliente por CPF com sucesso")
    void buscarPorCpf_Sucesso() {
        // Arrange
        Cliente cliente = new Cliente("12345678900", "Mariana Lima");
        cliente.setId(3L);
        when(clienteRepository.findByCpf("12345678900")).thenReturn(Optional.of(cliente));

        // Act
        ClienteResponseDTO resultado = clienteService.buscarPorCpf("12345678900");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getCpf()).isEqualTo("12345678900");
        assertThat(resultado.getNome()).isEqualTo("Mariana Lima");
        verify(clienteRepository, times(1)).findByCpf("12345678900");
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por CPF inexistente")
    void buscarPorCpf_NaoEncontrado() {
        // Arrange
        when(clienteRepository.findByCpf("00000000000")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clienteService.buscarPorCpf("00000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(clienteRepository, times(1)).findByCpf("00000000000");
    }

    @Test
    @DisplayName("Deve buscar clientes por nome parcial")
    void buscarPorNome_Sucesso() {
        // Arrange
        Cliente c1 = new Cliente("111.111.111-11", "Carlos Silva");
        c1.setId(1L);
        when(clienteRepository.findByNomeContainingIgnoreCase("carlos")).thenReturn(List.of(c1));

        // Act
        List<ClienteResponseDTO> resultado = clienteService.buscarPorNome("carlos");

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Carlos Silva");
        verify(clienteRepository, times(1)).findByNomeContainingIgnoreCase("carlos");
    }
}
