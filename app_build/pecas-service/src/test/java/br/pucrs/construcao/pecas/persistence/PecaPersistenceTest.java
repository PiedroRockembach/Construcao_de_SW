package br.pucrs.construcao.pecas.persistence;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.model.Peca;
import br.pucrs.construcao.pecas.service.PecaService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes da Camada de Persistência - Isolando JPA, Hibernate e Banco de Dados")
class PecaPersistenceTest {

    private InMemoryPecaRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPecaRepository();
    }

    @Test
    @DisplayName("Persistência: deve salvar peça gerando ID sequencial e consultar por ID")
    void deveSalvarEConsultarPorId() {
        Peca peca = new Peca("PEC-001", "Filtro de Ar", "Elemento filtrante");
        Peca salva = repository.save(peca);

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getId()).isEqualTo(1L);

        Optional<Peca> buscada = repository.findById(1L);
        assertThat(buscada).isPresent();
        assertThat(buscada.get().getNome()).isEqualTo("Filtro de Ar");
        assertThat(buscada.get().getNumeroIdentificacao()).isEqualTo("PEC-001");
    }

    @Test
    @DisplayName("Persistência: deve consultar por número de identificação")
    void deveBuscarPorNumeroIdentificacao() {
        repository.save(new Peca("PEC-100", "Correia Dentada", "Correia de distribuição"));

        Optional<Peca> encontrada = repository.findByNumeroIdentificacao("PEC-100");
        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNome()).isEqualTo("Correia Dentada");

        Optional<Peca> naoEncontrada = repository.findByNumeroIdentificacao("PEC-999");
        assertThat(naoEncontrada).isEmpty();
    }

    @Test
    @DisplayName("Persistência: deve verificar existência por número de identificação (existsByNumeroIdentificacao)")
    void deveVerificarExistenciaPorNumero() {
        repository.save(new Peca("PEC-200", "Vela Platina", "Vela de ignição"));

        assertThat(repository.existsByNumeroIdentificacao("PEC-200")).isTrue();
        assertThat(repository.existsByNumeroIdentificacao("PEC-999")).isFalse();
    }

    @Test
    @DisplayName("Persistência: deve buscar por nome contendo substring sem diferenciar maiúsculas/minúsculas")
    void deveBuscarPorNomeContainingIgnoreCase() {
        repository.save(new Peca("PEC-301", "Amortecedor Dianteiro", "Dianteiro"));
        repository.save(new Peca("PEC-302", "Amortecedor Traseiro", "Traseiro"));
        repository.save(new Peca("PEC-303", "Pastilha Freio", "Pastilha"));

        List<Peca> resultado = repository.findByNomeContainingIgnoreCase("amortecedor");
        assertThat(resultado).hasSize(2);

        List<Peca> resultadoFreio = repository.findByNomeContainingIgnoreCase("FREIO");
        assertThat(resultadoFreio).hasSize(1);
    }

    @Test
    @DisplayName("Persistência: deve deletar peça por ID e atualizar contagem")
    void deveDeletarPorId() {
        Peca peca = repository.save(new Peca("PEC-400", "Radiador", "Radiador de água"));
        Long id = peca.getId();

        assertThat(repository.existsById(id)).isTrue();
        assertThat(repository.count()).isEqualTo(1L);

        repository.deleteById(id);

        assertThat(repository.existsById(id)).isFalse();
        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Integração Isolada: PecaService funcionando 100% com repositório em memória sem nenhum banco de dados ou JPA")
    void servicoExecutaComRepositorioEmMemoriaSemBanco() {
        PecaService service = new PecaService(repository, new SimpleMeterRegistry());

        // 1. Cadastrar
        PecaResponseDTO criada = service.cadastrar(new PecaRequestDTO("PEC-900", "Bomba de Combustível", "Bomba elétrica"));
        assertThat(criada.getId()).isNotNull();
        assertThat(criada.getNumeroIdentificacao()).isEqualTo("PEC-900");

        // 2. Conflito
        assertThatThrownBy(() -> service.cadastrar(new PecaRequestDTO("PEC-900", "Bomba Duplicada", "Desc")))
                .isInstanceOf(ResponseStatusException.class);

        // 3. Listar
        List<PecaResponseDTO> todas = service.listarTodas();
        assertThat(todas).hasSize(1);

        // 4. Buscar por ID
        PecaResponseDTO buscaId = service.buscarPorId(criada.getId());
        assertThat(buscaId.getNome()).isEqualTo("Bomba de Combustível");
    }
}
