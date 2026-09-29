package br.pucrs.construcao.clientes.repository;

import br.pucrs.construcao.clientes.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste da camada de persistencia: @DataJpaTest sobe apenas o JPA sobre um H2
 * em memoria, e cada teste roda em transacao com rollback ao final. Valida as
 * query methods derivadas, que nao sao exercitadas pelos testes com mock.
 */
@DataJpaTest
@DisplayName("ClienteRepository - consultas derivadas sobre H2 em memoria")
class ClienteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    @DisplayName("save deve gerar o id e permitir recuperar o cliente")
    void saveDevePersistirEGerarId() {
        Cliente salvo = clienteRepository.save(new Cliente("12345678901", "Maria Silva"));
        entityManager.flush();
        entityManager.clear();

        assertThat(salvo.getId()).isNotNull();
        Optional<Cliente> encontrado = clienteRepository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("findByCpf deve encontrar o cliente cadastrado")
    void findByCpfDeveEncontrarCliente() {
        entityManager.persistAndFlush(new Cliente("12345678901", "Maria Silva"));

        assertThat(clienteRepository.findByCpf("12345678901"))
                .isPresent()
                .get()
                .extracting(Cliente::getNome)
                .isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("findByCpf deve retornar vazio para CPF inexistente")
    void findByCpfDeveRetornarVazio() {
        assertThat(clienteRepository.findByCpf("00000000000")).isEmpty();
    }

    @Test
    @DisplayName("findByNomeContainingIgnoreCase deve ignorar caixa e casar trechos")
    void findByNomeDeveIgnorarCaixa() {
        entityManager.persistAndFlush(new Cliente("12345678901", "Maria Silva"));
        entityManager.persistAndFlush(new Cliente("10987654321", "Mariana Souza"));
        entityManager.persistAndFlush(new Cliente("11122233344", "Joao Lima"));

        List<Cliente> encontrados = clienteRepository.findByNomeContainingIgnoreCase("mari");

        assertThat(encontrados)
                .extracting(Cliente::getNome)
                .containsExactlyInAnyOrder("Maria Silva", "Mariana Souza");
    }

    @Test
    @DisplayName("existsByCpf deve refletir a presenca do registro")
    void existsByCpfDeveRefletirBase() {
        entityManager.persistAndFlush(new Cliente("12345678901", "Maria Silva"));

        assertThat(clienteRepository.existsByCpf("12345678901")).isTrue();
        assertThat(clienteRepository.existsByCpf("00000000000")).isFalse();
    }

    @Test
    @DisplayName("a restricao de unicidade do CPF deve bloquear duplicatas")
    void cpfDuplicadoDeveViolarConstraint() {
        entityManager.persistAndFlush(new Cliente("12345678901", "Maria Silva"));

        assertThatThrownBy(() -> {
            clienteRepository.save(new Cliente("12345678901", "Outra Maria"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
