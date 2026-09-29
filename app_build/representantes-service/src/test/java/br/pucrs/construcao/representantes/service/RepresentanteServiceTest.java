package br.pucrs.construcao.representantes.service;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.model.Representante;
import br.pucrs.construcao.representantes.repository.RepresentanteRepository;
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
@DisplayName("RepresentanteService - regras de negocio isoladas do banco")
class RepresentanteServiceTest {

    @Mock
    private RepresentanteRepository representanteRepository;

    private MeterRegistry meterRegistry;
    private RepresentanteService representanteService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        representanteService = new RepresentanteService(representanteRepository, meterRegistry);
    }

    @Test
    @DisplayName("cadastrar deve salvar o representante, limpar espacos e contar sucesso")
    void cadastrarDevePersistirRepresentanteNovo() {
        when(representanteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(representanteRepository.save(any(Representante.class)))
                .thenAnswer(invocation -> {
                    Representante recebido = invocation.getArgument(0);
                    recebido.setId(1L);
                    return recebido;
                });

        RepresentanteResponseDTO resposta = representanteService.cadastrar(
                new RepresentanteRequestDTO("  12345678901  ", "  Maria Silva  "));

        ArgumentCaptor<Representante> captor = ArgumentCaptor.forClass(Representante.class);
        verify(representanteRepository).save(captor.capture());
        assertThat(captor.getValue().getCpf()).isEqualTo("12345678901");
        assertThat(captor.getValue().getNome()).isEqualTo("Maria Silva");

        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNome()).isEqualTo("Maria Silva");
        assertThat(contador("representantes.cadastro", "sucesso")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cadastrar deve lancar 409 quando o CPF ja existe e nao salvar nada")
    void cadastrarDeveRejeitarCpfDuplicado() {
        when(representanteRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> representanteService.cadastrar(
                new RepresentanteRequestDTO("12345678901", "Maria Silva")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("12345678901")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(representanteRepository, never()).save(any(Representante.class));
        assertThat(contador("representantes.cadastro", "conflito")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("listarTodos deve converter as entidades em DTOs")
    void listarTodosDeveMapearEntidades() {
        when(representanteRepository.findAll()).thenReturn(List.of(
                representante(1L, "12345678901", "Maria Silva"),
                representante(2L, "10987654321", "Joao Souza")));

        List<RepresentanteResponseDTO> resposta = representanteService.listarTodos();

        assertThat(resposta).hasSize(2)
                .extracting(RepresentanteResponseDTO::getNome)
                .containsExactly("Maria Silva", "Joao Souza");
    }

    @Test
    @DisplayName("buscarPorId deve retornar o representante existente")
    void buscarPorIdDeveRetornarRepresentante() {
        when(representanteRepository.findById(1L))
                .thenReturn(Optional.of(representante(1L, "12345678901", "Maria Silva")));

        RepresentanteResponseDTO resposta = representanteService.buscarPorId(1L);

        assertThat(resposta.getCpf()).isEqualTo("12345678901");
        assertThat(contador("representantes.consulta.nao_encontrada", null)).isZero();
    }

    @Test
    @DisplayName("buscarPorId deve lancar 404 quando o representante nao existe")
    void buscarPorIdDeveLancarNotFound() {
        when(representanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("representantes.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorCpf deve consultar o repositorio com o CPF sem espacos")
    void buscarPorCpfDeveNormalizarEntrada() {
        when(representanteRepository.findByCpf("12345678901"))
                .thenReturn(Optional.of(representante(1L, "12345678901", "Maria Silva")));

        RepresentanteResponseDTO resposta = representanteService.buscarPorCpf("  12345678901  ");

        assertThat(resposta.getId()).isEqualTo(1L);
        verify(representanteRepository).findByCpf("12345678901");
    }

    @Test
    @DisplayName("buscarPorCpf deve lancar 404 quando nao ha correspondencia")
    void buscarPorCpfDeveLancarNotFound() {
        when(representanteRepository.findByCpf(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> representanteService.buscarPorCpf("00000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("representantes.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorNome deve retornar representantes mapeados quando houver correspondencia")
    void buscarPorNomeDeveRetornarRepresentantesEncontrados() {
        when(representanteRepository.findByNomeContainingIgnoreCase("Silva"))
                .thenReturn(List.of(representante(1L, "12345678901", "Maria Silva")));

        List<RepresentanteResponseDTO> resposta = representanteService.buscarPorNome("Silva");

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).getCpf()).isEqualTo("12345678901");
        assertThat(resposta.get(0).getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("buscarPorNome deve retornar lista vazia quando nada casa com o filtro")
    void buscarPorNomeDeveRetornarListaVazia() {
        when(representanteRepository.findByNomeContainingIgnoreCase("ana")).thenReturn(List.of());

        assertThat(representanteService.buscarPorNome("  ana  ")).isEmpty();
        verify(representanteRepository).findByNomeContainingIgnoreCase("ana");
    }

    @Test
    @DisplayName("gauge de registros deve refletir a contagem do repositorio")
    void gaugeDeveRefletirContagemDoRepositorio() {
        when(representanteRepository.count()).thenReturn(4L);
        double valorGauge = meterRegistry.get("representantes.registros").gauge().value();
        assertThat(valorGauge).isEqualTo(4.0);
    }

    private double contador(String nome, String resultado) {
        return resultado == null
                ? meterRegistry.counter(nome).count()
                : meterRegistry.counter(nome, "resultado", resultado).count();
    }

    private Representante representante(Long id, String cpf, String nome) {
        Representante representante = new Representante(cpf, nome);
        representante.setId(id);
        return representante;
    }
}
