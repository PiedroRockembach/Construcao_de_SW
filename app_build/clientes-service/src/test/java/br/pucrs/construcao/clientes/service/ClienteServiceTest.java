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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitario do servico: a unica dependencia real e o MeterRegistry em
 * memoria (SimpleMeterRegistry); o repositorio - e portanto o banco - e
 * substituido por um mock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - regras de negocio isoladas do banco")
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
    @DisplayName("cadastrar deve salvar o cliente, limpar espacos e contar sucesso")
    void cadastrarDevePersistirClienteNovo() {
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> {
                    Cliente recebido = invocation.getArgument(0);
                    recebido.setId(1L);
                    return recebido;
                });

        ClienteResponseDTO resposta = clienteService.cadastrar(
                new ClienteRequestDTO("  12345678901  ", "  Maria Silva  "));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getCpf()).isEqualTo("12345678901");
        assertThat(captor.getValue().getNome()).isEqualTo("Maria Silva");

        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNome()).isEqualTo("Maria Silva");
        assertThat(contador("clientes.cadastro", "sucesso")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cadastrar deve lancar 409 quando o CPF ja existe e nao salvar nada")
    void cadastrarDeveRejeitarCpfDuplicado() {
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.cadastrar(
                new ClienteRequestDTO("12345678901", "Maria Silva")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("12345678901")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any(Cliente.class));
        assertThat(contador("clientes.cadastro", "conflito")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("listarTodos deve converter as entidades em DTOs")
    void listarTodosDeveMapearEntidades() {
        when(clienteRepository.findAll()).thenReturn(List.of(
                cliente(1L, "12345678901", "Maria Silva"),
                cliente(2L, "10987654321", "Joao Souza")));

        List<ClienteResponseDTO> resposta = clienteService.listarTodos();

        assertThat(resposta).hasSize(2)
                .extracting(ClienteResponseDTO::getNome)
                .containsExactly("Maria Silva", "Joao Souza");
    }

    @Test
    @DisplayName("buscarPorId deve retornar o cliente existente")
    void buscarPorIdDeveRetornarCliente() {
        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(cliente(1L, "12345678901", "Maria Silva")));

        ClienteResponseDTO resposta = clienteService.buscarPorId(1L);

        assertThat(resposta.getCpf()).isEqualTo("12345678901");
        assertThat(contador("clientes.consulta.nao_encontrada", null)).isZero();
    }

    @Test
    @DisplayName("buscarPorId deve lancar 404 quando o cliente nao existe")
    void buscarPorIdDeveLancarNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("clientes.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorCpf deve consultar o repositorio com o CPF sem espacos")
    void buscarPorCpfDeveNormalizarEntrada() {
        when(clienteRepository.findByCpf("12345678901"))
                .thenReturn(Optional.of(cliente(1L, "12345678901", "Maria Silva")));

        ClienteResponseDTO resposta = clienteService.buscarPorCpf("  12345678901  ");

        assertThat(resposta.getId()).isEqualTo(1L);
        verify(clienteRepository).findByCpf("12345678901");
    }

    @Test
    @DisplayName("buscarPorCpf deve lancar 404 quando nao ha correspondencia")
    void buscarPorCpfDeveLancarNotFound() {
        when(clienteRepository.findByCpf(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorCpf("00000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("clientes.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorNome deve retornar clientes mapeados quando houver correspondencia")
    void buscarPorNomeDeveRetornarClientesEncontrados() {
        when(clienteRepository.findByNomeContainingIgnoreCase("Silva"))
                .thenReturn(List.of(cliente(1L, "12345678901", "Maria Silva")));

        List<ClienteResponseDTO> resposta = clienteService.buscarPorNome("Silva");

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).getCpf()).isEqualTo("12345678901");
        assertThat(resposta.get(0).getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("buscarPorNome deve retornar lista vazia quando nada casa com o filtro")
    void buscarPorNomeDeveRetornarListaVazia() {
        when(clienteRepository.findByNomeContainingIgnoreCase("ana")).thenReturn(List.of());

        assertThat(clienteService.buscarPorNome("  ana  ")).isEmpty();
        verify(clienteRepository).findByNomeContainingIgnoreCase("ana");
    }

    @Test
    @DisplayName("gauge de registros deve refletir a contagem do repositorio")
    void gaugeDeveRefletirContagemDoRepositorio() {
        when(clienteRepository.count()).thenReturn(3L);
        double valorGauge = meterRegistry.get("clientes.registros").gauge().value();
        assertThat(valorGauge).isEqualTo(3.0);
    }

    private double contador(String nome, String resultado) {
        return resultado == null
                ? meterRegistry.counter(nome).count()
                : meterRegistry.counter(nome, "resultado", resultado).count();
    }

    private Cliente cliente(Long id, String cpf, String nome) {
        Cliente cliente = new Cliente(cpf, nome);
        cliente.setId(id);
        return cliente;
    }
}
