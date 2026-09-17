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
@DisplayName("Testes Unitários de RepresentanteService")
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
    @DisplayName("Deve cadastrar representante com sucesso quando CPF não existir")
    void cadastrar_Sucesso() {
        // Arrange
        RepresentanteRequestDTO dto = new RepresentanteRequestDTO("123.456.789-00", "Roberto Santos");
        when(representanteRepository.existsByCpf("123.456.789-00")).thenReturn(false);

        Representante salvo = new Representante("123.456.789-00", "Roberto Santos");
        salvo.setId(1L);
        when(representanteRepository.save(any(Representante.class))).thenReturn(salvo);

        // Act
        RepresentanteResponseDTO resultado = representanteService.cadastrar(dto);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCpf()).isEqualTo("123.456.789-00");
        assertThat(resultado.getNome()).isEqualTo("Roberto Santos");

        double count = meterRegistry.get("representantes.created.total").counter().count();
        assertThat(count).isEqualTo(1.0);

        verify(representanteRepository, times(1)).existsByCpf("123.456.789-00");
        verify(representanteRepository, times(1)).save(any(Representante.class));
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 ao cadastrar representante com CPF existente")
    void cadastrar_ConflitoCpf() {
        // Arrange
        RepresentanteRequestDTO dto = new RepresentanteRequestDTO("123.456.789-00", "Roberto Santos");
        when(representanteRepository.existsByCpf("123.456.789-00")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> representanteService.cadastrar(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("Já existe um representante cadastrado");
                });

        verify(representanteRepository, times(1)).existsByCpf("123.456.789-00");
        verify(representanteRepository, never()).save(any(Representante.class));
    }

    @Test
    @DisplayName("Deve listar todos os representantes")
    void listarTodos_Sucesso() {
        // Arrange
        Representante r1 = new Representante("111.111.111-11", "Juliana Lima");
        r1.setId(1L);
        Representante r2 = new Representante("222.222.222-22", "Marcos Paulo");
        r2.setId(2L);

        when(representanteRepository.findAll()).thenReturn(List.of(r1, r2));

        // Act
        List<RepresentanteResponseDTO> resultado = representanteService.listarTodos();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNome()).isEqualTo("Juliana Lima");
        assertThat(resultado.get(1).getNome()).isEqualTo("Marcos Paulo");
        verify(representanteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar representante por ID com sucesso")
    void buscarPorId_Sucesso() {
        // Arrange
        Representante rep = new Representante("123.456.789-00", "Roberto Santos");
        rep.setId(10L);
        when(representanteRepository.findById(10L)).thenReturn(Optional.of(rep));

        // Act
        RepresentanteResponseDTO resultado = representanteService.buscarPorId(10L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getNome()).isEqualTo("Roberto Santos");
        verify(representanteRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por ID inexistente")
    void buscarPorId_NaoEncontrado() {
        // Arrange
        when(representanteRepository.findById(88L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> representanteService.buscarPorId(88L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Representante não encontrado com o ID: 88");
                });

        verify(representanteRepository, times(1)).findById(88L);
    }

    @Test
    @DisplayName("Deve buscar representante por CPF com sucesso")
    void buscarPorCpf_Sucesso() {
        // Arrange
        Representante rep = new Representante("12345678900", "Juliana Lima");
        rep.setId(5L);
        when(representanteRepository.findByCpf("12345678900")).thenReturn(Optional.of(rep));

        // Act
        RepresentanteResponseDTO resultado = representanteService.buscarPorCpf("12345678900");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getCpf()).isEqualTo("12345678900");
        assertThat(resultado.getNome()).isEqualTo("Juliana Lima");
        verify(representanteRepository, times(1)).findByCpf("12345678900");
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar por CPF inexistente")
    void buscarPorCpf_NaoEncontrado() {
        // Arrange
        when(representanteRepository.findByCpf("00000000000")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> representanteService.buscarPorCpf("00000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(representanteRepository, times(1)).findByCpf("00000000000");
    }

    @Test
    @DisplayName("Deve buscar representantes por nome parcial")
    void buscarPorNome_Sucesso() {
        // Arrange
        Representante r1 = new Representante("111.111.111-11", "Juliana Lima");
        r1.setId(1L);
        when(representanteRepository.findByNomeContainingIgnoreCase("juliana")).thenReturn(List.of(r1));

        // Act
        List<RepresentanteResponseDTO> resultado = representanteService.buscarPorNome("juliana");

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Juliana Lima");
        verify(representanteRepository, times(1)).findByNomeContainingIgnoreCase("juliana");
    }
}
