package br.pucrs.construcao.representantes.repository;

import br.pucrs.construcao.representantes.model.Representante;
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
@DisplayName("RepresentanteRepository - consultas derivadas sobre H2 em memoria")
class RepresentanteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RepresentanteRepository representanteRepository;

    @Test
    @DisplayName("save deve gerar o id e permitir recuperar o representante")
    void saveDevePersistirEGerarId() {
        Representante salvo = representanteRepository.save(new Representante("12345678901", "Maria Silva"));
        entityManager.flush();
        entityManager.clear();

        assertThat(salvo.getId()).isNotNull();
        Optional<Representante> encontrado = representanteRepository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("findByCpf deve encontrar o representante cadastrado")
    void findByCpfDeveEncontrarRepresentante() {
        entityManager.persistAndFlush(new Representante("12345678901", "Maria Silva"));

        assertThat(representanteRepository.findByCpf("12345678901"))
                .isPresent()
                .get()
                .extracting(Representante::getNome)
                .isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("findByCpf deve retornar vazio para CPF inexistente")
    void findByCpfDeveRetornarVazio() {
        assertThat(representanteRepository.findByCpf("00000000000")).isEmpty();
    }

    @Test
    @DisplayName("findByNomeContainingIgnoreCase deve ignorar caixa e casar trechos")
    void findByNomeDeveIgnorarCaixa() {
        entityManager.persistAndFlush(new Representante("12345678901", "Maria Silva"));
        entityManager.persistAndFlush(new Representante("10987654321", "Mariana Souza"));
        entityManager.persistAndFlush(new Representante("11122233344", "Joao Lima"));

        List<Representante> encontrados = representanteRepository.findByNomeContainingIgnoreCase("mari");

        assertThat(encontrados)
                .extracting(Representante::getNome)
                .containsExactlyInAnyOrder("Maria Silva", "Mariana Souza");
    }

    @Test
    @DisplayName("existsByCpf deve refletir a presenca do registro")
    void existsByCpfDeveRefletirBase() {
        entityManager.persistAndFlush(new Representante("12345678901", "Maria Silva"));

        assertThat(representanteRepository.existsByCpf("12345678901")).isTrue();
        assertThat(representanteRepository.existsByCpf("00000000000")).isFalse();
    }

    @Test
    @DisplayName("a restricao de unicidade do CPF deve bloquear duplicatas")
    void cpfDuplicadoDeveViolarConstraint() {
        entityManager.persistAndFlush(new Representante("12345678901", "Maria Silva"));

        assertThatThrownBy(() -> {
            representanteRepository.save(new Representante("12345678901", "Outra Maria"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
