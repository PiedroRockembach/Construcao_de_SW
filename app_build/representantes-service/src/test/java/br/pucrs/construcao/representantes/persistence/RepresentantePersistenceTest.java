package br.pucrs.construcao.representantes.persistence;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.model.Representante;
import br.pucrs.construcao.representantes.service.RepresentanteService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes da Camada de Persistência de Representantes - Isolando JPA, Hibernate e BD")
class RepresentantePersistenceTest {

    private InMemoryRepresentanteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepresentanteRepository();
    }

    @Test
    @DisplayName("Persistência: deve salvar representante gerando ID sequencial e consultar por ID")
    void deveSalvarEConsultarPorId() {
        Representante rep = new Representante("123.456.789-00", "Roberto Santos");
        Representante salvo = repository.save(rep);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getId()).isEqualTo(1L);

        Optional<Representante> buscado = repository.findById(1L);
        assertThat(buscado).isPresent();
        assertThat(buscado.get().getNome()).isEqualTo("Roberto Santos");
        assertThat(buscado.get().getCpf()).isEqualTo("123.456.789-00");
    }

    @Test
    @DisplayName("Persistência: deve consultar por CPF")
    void deveBuscarPorCpf() {
        repository.save(new Representante("111.222.333-44", "Juliana Lima"));

        Optional<Representante> encontrado = repository.findByCpf("111.222.333-44");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Juliana Lima");

        Optional<Representante> naoEncontrado = repository.findByCpf("999.999.999-99");
        assertThat(naoEncontrado).isEmpty();
    }

    @Test
    @DisplayName("Persistência: deve verificar existência por CPF (existsByCpf)")
    void deveVerificarExistenciaPorCpf() {
        repository.save(new Representante("222.333.444-55", "Marcos Paulo"));

        assertThat(repository.existsByCpf("222.333.444-55")).isTrue();
        assertThat(repository.existsByCpf("000.000.000-00")).isFalse();
    }

    @Test
    @DisplayName("Persistência: deve buscar por nome contendo substring sem diferenciar maiúsculas/minúsculas")
    void deveBuscarPorNomeContainingIgnoreCase() {
        repository.save(new Representante("111", "Juliana Lima"));
        repository.save(new Representante("222", "Julia Roberts"));
        repository.save(new Representante("333", "Marcos Paulo"));

        List<Representante> resultado = repository.findByNomeContainingIgnoreCase("juli");
        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("Persistência: deve deletar representante por ID")
    void deveDeletarPorId() {
        Representante salvo = repository.save(new Representante("123", "Teste"));
        Long id = salvo.getId();

        assertThat(repository.existsById(id)).isTrue();
        repository.deleteById(id);
        assertThat(repository.existsById(id)).isFalse();
        assertThat(repository.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Integração Isolada: RepresentanteService executando com repositório em memória sem dependência de banco de dados ou JPA")
    void servicoExecutaComRepositorioEmMemoriaSemBanco() {
        RepresentanteService service = new RepresentanteService(repository, new SimpleMeterRegistry());

        // 1. Cadastrar
        RepresentanteResponseDTO criado = service.cadastrar(new RepresentanteRequestDTO("123.456.789-00", "Roberto"));
        assertThat(criado.getId()).isNotNull();
        assertThat(criado.getNome()).isEqualTo("Roberto");

        // 2. Conflito
        assertThatThrownBy(() -> service.cadastrar(new RepresentanteRequestDTO("123.456.789-00", "Roberto Duplicado")))
                .isInstanceOf(ResponseStatusException.class);

        // 3. Listar
        List<RepresentanteResponseDTO> todos = service.listarTodos();
        assertThat(todos).hasSize(1);

        // 4. Buscar por CPF
        RepresentanteResponseDTO porCpf = service.buscarPorCpf("123.456.789-00");
        assertThat(porCpf.getNome()).isEqualTo("Roberto");
    }
}
