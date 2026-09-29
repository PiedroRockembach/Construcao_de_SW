package br.pucrs.construcao.pecas.repository;

import br.pucrs.construcao.pecas.model.Peca;
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
@DisplayName("PecaRepository - consultas derivadas sobre H2 em memoria")
class PecaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PecaRepository pecaRepository;

    @Test
    @DisplayName("save deve gerar o id e permitir recuperar a peca")
    void saveDevePersistirEGerarId() {
        Peca salva = pecaRepository.save(new Peca("PN-001", "Parafuso M8", "Sextavado"));
        entityManager.flush();
        entityManager.clear();

        assertThat(salva.getId()).isNotNull();
        Optional<Peca> encontrada = pecaRepository.findById(salva.getId());
        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getDescricao()).isEqualTo("Sextavado");
    }

    @Test
    @DisplayName("findByNumeroIdentificacao deve encontrar a peca cadastrada")
    void findByNumeroDeveEncontrarPeca() {
        entityManager.persistAndFlush(new Peca("PN-001", "Parafuso M8", "Sextavado"));

        assertThat(pecaRepository.findByNumeroIdentificacao("PN-001"))
                .isPresent()
                .get()
                .extracting(Peca::getNome)
                .isEqualTo("Parafuso M8");
    }

    @Test
    @DisplayName("findByNumeroIdentificacao deve retornar vazio para numero inexistente")
    void findByNumeroDeveRetornarVazio() {
        assertThat(pecaRepository.findByNumeroIdentificacao("PN-999")).isEmpty();
    }

    @Test
    @DisplayName("findByNomeContainingIgnoreCase deve ignorar caixa e casar trechos")
    void findByNomeDeveIgnorarCaixa() {
        entityManager.persistAndFlush(new Peca("PN-001", "Parafuso M8", null));
        entityManager.persistAndFlush(new Peca("PN-002", "Parafuso M10", null));
        entityManager.persistAndFlush(new Peca("PN-003", "Porca M8", null));

        List<Peca> encontradas = pecaRepository.findByNomeContainingIgnoreCase("parafuso");

        assertThat(encontradas)
                .extracting(Peca::getNome)
                .containsExactlyInAnyOrder("Parafuso M8", "Parafuso M10");
    }

    @Test
    @DisplayName("existsByNumeroIdentificacao deve refletir a presenca do registro")
    void existsByNumeroDeveRefletirBase() {
        entityManager.persistAndFlush(new Peca("PN-001", "Parafuso M8", null));

        assertThat(pecaRepository.existsByNumeroIdentificacao("PN-001")).isTrue();
        assertThat(pecaRepository.existsByNumeroIdentificacao("PN-999")).isFalse();
    }

    @Test
    @DisplayName("a restricao de unicidade do numero de identificacao deve bloquear duplicatas")
    void numeroDuplicadoDeveViolarConstraint() {
        entityManager.persistAndFlush(new Peca("PN-001", "Parafuso M8", null));

        assertThatThrownBy(() -> {
            pecaRepository.save(new Peca("PN-001", "Outro Parafuso", null));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
