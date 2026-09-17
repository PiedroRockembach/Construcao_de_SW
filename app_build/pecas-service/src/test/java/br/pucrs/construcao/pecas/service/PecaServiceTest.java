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
@DisplayName("Testes Unitários de PecaService")
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
    @DisplayName("Deve cadastrar peça com sucesso quando número de identificação não existir")
    void cadastrar_Sucesso() {
        // Arrange
        PecaRequestDTO dto = new PecaRequestDTO("PEC-001", "Amortecedor Dianteiro", "Amortecedor a gás");
        when(pecaRepository.existsByNumeroIdentificacao("PEC-001")).thenReturn(false);

        Peca pecaSalva = new Peca("PEC-001", "Amortecedor Dianteiro", "Amortecedor a gás");
        pecaSalva.setId(1L);
        when(pecaRepository.save(any(Peca.class))).thenReturn(pecaSalva);

        // Act
        PecaResponseDTO resultado = pecaService.cadastrar(dto);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNumeroIdentificacao()).isEqualTo("PEC-001");
        assertThat(resultado.getNome()).isEqualTo("Amortecedor Dianteiro");
        assertThat(resultado.getDescricao()).isEqualTo("Amortecedor a gás");

        // Verifica incremento da métrica
        double count = meterRegistry.get("pecas.created.total").counter().count();
        assertThat(count).isEqualTo(1.0);

        verify(pecaRepository, times(1)).existsByNumeroIdentificacao("PEC-001");
        verify(pecaRepository, times(1)).save(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 ao cadastrar peça com número de identificação já existente")
    void cadastrar_ConflitoNumeroIdentificacao() {
        // Arrange
        PecaRequestDTO dto = new PecaRequestDTO("PEC-001", "Amortecedor Dianteiro", "Amortecedor a gás");
        when(pecaRepository.existsByNumeroIdentificacao("PEC-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> pecaService.cadastrar(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("Já existe uma peça cadastrada");
                });

        verify(pecaRepository, times(1)).existsByNumeroIdentificacao("PEC-001");
        verify(pecaRepository, never()).save(any(Peca.class));
    }

    @Test
    @DisplayName("Deve listar todas as peças cadastradas")
    void listarTodas_Sucesso() {
        // Arrange
        Peca p1 = new Peca("PEC-001", "Amortecedor", "Desc 1");
        p1.setId(1L);
        Peca p2 = new Peca("PEC-002", "Pastilha de Freio", "Desc 2");
        p2.setId(2L);

        when(pecaRepository.findAll()).thenReturn(List.of(p1, p2));

        // Act
        List<PecaResponseDTO> resultado = pecaService.listarTodas();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNumeroIdentificacao()).isEqualTo("PEC-001");
        assertThat(resultado.get(1).getNumeroIdentificacao()).isEqualTo("PEC-002");
        verify(pecaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar peça por ID com sucesso quando existir")
    void buscarPorId_Sucesso() {
        // Arrange
        Peca peca = new Peca("PEC-001", "Disco de Freio", "Desc");
        peca.setId(10L);
        when(pecaRepository.findById(10L)).thenReturn(Optional.of(peca));

        // Act
        PecaResponseDTO resultado = pecaService.buscarPorId(10L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getNome()).isEqualTo("Disco de Freio");
        verify(pecaRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por ID inexistente")
    void buscarPorId_NaoEncontrado() {
        // Arrange
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pecaService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Peça não encontrada com ID: 99");
                });

        verify(pecaRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve buscar peça por número de identificação com sucesso")
    void buscarPorNumeroIdentificacao_Sucesso() {
        // Arrange
        Peca peca = new Peca("PEC-123", "Filtro de Óleo", "Desc");
        peca.setId(5L);
        when(pecaRepository.findByNumeroIdentificacao("PEC-123")).thenReturn(Optional.of(peca));

        // Act
        PecaResponseDTO resultado = pecaService.buscarPorNumeroIdentificacao("PEC-123");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroIdentificacao()).isEqualTo("PEC-123");
        verify(pecaRepository, times(1)).findByNumeroIdentificacao("PEC-123");
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por número de identificação inexistente")
    void buscarPorNumeroIdentificacao_NaoEncontrado() {
        // Arrange
        when(pecaRepository.findByNumeroIdentificacao("INEXISTENTE")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pecaService.buscarPorNumeroIdentificacao("INEXISTENTE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(pecaRepository, times(1)).findByNumeroIdentificacao("INEXISTENTE");
    }

    @Test
    @DisplayName("Deve buscar peças por nome parcial ignorando maiúsculas/minúsculas")
    void buscarPorNome_Sucesso() {
        // Arrange
        Peca p1 = new Peca("PEC-010", "Filtro de Ar", "Desc");
        p1.setId(1L);
        Peca p2 = new Peca("PEC-011", "Filtro de Combustível", "Desc");
        p2.setId(2L);

        when(pecaRepository.findByNomeContainingIgnoreCase("filtro")).thenReturn(List.of(p1, p2));

        // Act
        List<PecaResponseDTO> resultado = pecaService.buscarPorNome("filtro");

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNome()).isEqualTo("Filtro de Ar");
        assertThat(resultado.get(1).getNome()).isEqualTo("Filtro de Combustível");
        verify(pecaRepository, times(1)).findByNomeContainingIgnoreCase("filtro");
    }
}
