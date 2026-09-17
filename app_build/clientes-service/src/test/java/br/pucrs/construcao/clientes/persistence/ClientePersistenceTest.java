package br.pucrs.construcao.clientes.persistence;

import br.pucrs.construcao.clientes.dto.ClienteRequestDTO;
import br.pucrs.construcao.clientes.dto.ClienteResponseDTO;
import br.pucrs.construcao.clientes.model.Cliente;
import br.pucrs.construcao.clientes.service.ClienteService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes da Camada de Persistência de Clientes - Isolando JPA, Hibernate e BD")
class ClientePersistenceTest {

    private InMemoryClienteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryClienteRepository();
    }

    @Test
    @DisplayName("Persistência: deve salvar cliente gerando ID sequencial e consultar por ID")
    void deveSalvarEConsultarPorId() {
        Cliente cliente = new Cliente("123.456.789-00", "Carlos Silva");
        Cliente salvo = repository.save(cliente);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getId()).isEqualTo(1L);

        Optional<Cliente> buscado = repository.findById(1L);
        assertThat(buscado).isPresent();
        assertThat(buscado.get().getNome()).isEqualTo("Carlos Silva");
        assertThat(buscado.get().getCpf()).isEqualTo("123.456.789-00");
    }

    @Test
    @DisplayName("Persistência: deve consultar por CPF")
    void deveBuscarPorCpf() {
        repository.save(new Cliente("111.222.333-44", "Lucas Lima"));

        Optional<Cliente> encontrado = repository.findByCpf("111.222.333-44");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Lucas Lima");

        Optional<Cliente> naoEncontrado = repository.findByCpf("999.999.999-99");
        assertThat(naoEncontrado).isEmpty();
    }

    @Test
    @DisplayName("Persistência: deve verificar existência por CPF (existsByCpf)")
    void deveVerificarExistenciaPorCpf() {
        repository.save(new Cliente("222.333.444-55", "Mariana"));

        assertThat(repository.existsByCpf("222.333.444-55")).isTrue();
        assertThat(repository.existsByCpf("000.000.000-00")).isFalse();
    }

    @Test
    @DisplayName("Persistência: deve buscar por nome contendo substring")
    void deveBuscarPorNomeContainingIgnoreCase() {
        repository.save(new Cliente("111", "Ana Maria Braga"));
        repository.save(new Cliente("222", "Mariana Rios"));
        repository.save(new Cliente("333", "Carlos Eduardo"));

        List<Cliente> resultado = repository.findByNomeContainingIgnoreCase("mari");
        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("Persistência: deve deletar cliente por ID")
    void deveDeletarPorId() {
        Cliente salvo = repository.save(new Cliente("123", "Teste"));
        Long id = salvo.getId();

        assertThat(repository.existsById(id)).isTrue();
        repository.deleteById(id);
        assertThat(repository.existsById(id)).isFalse();
        assertThat(repository.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Integração Isolada: ClienteService executando com repositório em memória sem dependência de banco de dados ou JPA")
    void servicoExecutaComRepositorioEmMemoriaSemBanco() {
        ClienteService service = new ClienteService(repository, new SimpleMeterRegistry());

        // 1. Cadastrar
        ClienteResponseDTO criado = service.cadastrar(new ClienteRequestDTO("123.456.789-00", "Carlos"));
        assertThat(criado.getId()).isNotNull();
        assertThat(criado.getNome()).isEqualTo("Carlos");

        // 2. Conflito
        assertThatThrownBy(() -> service.cadastrar(new ClienteRequestDTO("123.456.789-00", "Carlos Duplicado")))
                .isInstanceOf(ResponseStatusException.class);

        // 3. Listar
        List<ClienteResponseDTO> todos = service.listarTodos();
        assertThat(todos).hasSize(1);

        // 4. Buscar por CPF
        ClienteResponseDTO porCpf = service.buscarPorCpf("123.456.789-00");
        assertThat(porCpf.getNome()).isEqualTo("Carlos");
    }
}
