package br.pucrs.construcao.pecas.service;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.model.Peca;
import br.pucrs.construcao.pecas.repository.PecaRepository;
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
@DisplayName("PecaService - regras de negocio isoladas do banco")
class PecaServiceTest {

    @Mock
    private PecaRepository pecaRepository;

    private MeterRegistry meterRegistry;
    private PecaService pecaService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        pecaService = new PecaService(pecaRepository, meterRegistry);
    }

    @Test
    @DisplayName("cadastrar deve salvar a peca e contar sucesso")
    void cadastrarDevePersistirPecaNova() {
        when(pecaRepository.existsByNumeroIdentificacao("PN-001")).thenReturn(false);
        when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> {
            Peca recebida = invocation.getArgument(0);
            recebida.setId(1L);
            return recebida;
        });

        PecaResponseDTO resposta = pecaService.cadastrar(
                new PecaRequestDTO("PN-001", "Parafuso M8", "Parafuso sextavado"));

        ArgumentCaptor<Peca> captor = ArgumentCaptor.forClass(Peca.class);
        verify(pecaRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroIdentificacao()).isEqualTo("PN-001");
        assertThat(captor.getValue().getDescricao()).isEqualTo("Parafuso sextavado");

        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNome()).isEqualTo("Parafuso M8");
        assertThat(contador("pecas.cadastro", "sucesso")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cadastrar deve lancar 409 quando o numero de identificacao ja existe")
    void cadastrarDeveRejeitarNumeroDuplicado() {
        when(pecaRepository.existsByNumeroIdentificacao("PN-001")).thenReturn(true);

        assertThatThrownBy(() -> pecaService.cadastrar(
                new PecaRequestDTO("PN-001", "Parafuso M8", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PN-001")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(pecaRepository, never()).save(any(Peca.class));
        assertThat(contador("pecas.cadastro", "conflito")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("listarTodas deve converter as entidades em DTOs")
    void listarTodasDeveMapearEntidades() {
        when(pecaRepository.findAll()).thenReturn(List.of(
                peca(1L, "PN-001", "Parafuso M8", "Sextavado"),
                peca(2L, "PN-002", "Porca M8", "Galvanizada")));

        List<PecaResponseDTO> resposta = pecaService.listarTodas();

        assertThat(resposta).hasSize(2)
                .extracting(PecaResponseDTO::getNumeroIdentificacao)
                .containsExactly("PN-001", "PN-002");
    }

    @Test
    @DisplayName("buscarPorId deve retornar a peca existente")
    void buscarPorIdDeveRetornarPeca() {
        when(pecaRepository.findById(1L))
                .thenReturn(Optional.of(peca(1L, "PN-001", "Parafuso M8", "Sextavado")));

        PecaResponseDTO resposta = pecaService.buscarPorId(1L);

        assertThat(resposta.getNome()).isEqualTo("Parafuso M8");
        assertThat(contador("pecas.consulta.nao_encontrada", null)).isZero();
    }

    @Test
    @DisplayName("buscarPorId deve lancar 404 quando a peca nao existe")
    void buscarPorIdDeveLancarNotFound() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pecaService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("pecas.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorNumeroIdentificacao deve delegar ao repositorio")
    void buscarPorNumeroDeveRetornarPeca() {
        when(pecaRepository.findByNumeroIdentificacao("PN-001"))
                .thenReturn(Optional.of(peca(1L, "PN-001", "Parafuso M8", "Sextavado")));

        assertThat(pecaService.buscarPorNumeroIdentificacao("PN-001").getId()).isEqualTo(1L);
        verify(pecaRepository).findByNumeroIdentificacao("PN-001");
    }

    @Test
    @DisplayName("buscarPorNumeroIdentificacao deve lancar 404 quando nao ha correspondencia")
    void buscarPorNumeroDeveLancarNotFound() {
        when(pecaRepository.findByNumeroIdentificacao(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pecaService.buscarPorNumeroIdentificacao("PN-999"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contador("pecas.consulta.nao_encontrada", null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("buscarPorNome deve retornar pecas mapeadas quando houver correspondencia")
    void buscarPorNomeDeveRetornarPecasEncontradas() {
        when(pecaRepository.findByNomeContainingIgnoreCase("Parafuso"))
                .thenReturn(List.of(peca(1L, "PN-001", "Parafuso M8", "Sextavado")));

        List<PecaResponseDTO> resposta = pecaService.buscarPorNome("Parafuso");

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).getNumeroIdentificacao()).isEqualTo("PN-001");
        assertThat(resposta.get(0).getNome()).isEqualTo("Parafuso M8");
    }

    @Test
    @DisplayName("buscarPorNome deve retornar lista vazia quando nada casa com o filtro")
    void buscarPorNomeDeveRetornarListaVazia() {
        when(pecaRepository.findByNomeContainingIgnoreCase("xyz")).thenReturn(List.of());

        assertThat(pecaService.buscarPorNome("xyz")).isEmpty();
        verify(pecaRepository).findByNomeContainingIgnoreCase("xyz");
    }

    @Test
    @DisplayName("gauge de registros deve refletir a contagem do repositorio")
    void gaugeDeveRefletirContagemDoRepositorio() {
        when(pecaRepository.count()).thenReturn(5L);
        double valorGauge = meterRegistry.get("pecas.registros").gauge().value();
        assertThat(valorGauge).isEqualTo(5.0);
    }

    private double contador(String nome, String resultado) {
        return resultado == null
                ? meterRegistry.counter(nome).count()
                : meterRegistry.counter(nome, "resultado", resultado).count();
    }

    private Peca peca(Long id, String numero, String nome, String descricao) {
        Peca peca = new Peca(numero, nome, descricao);
        peca.setId(id);
        return peca;
    }
}
