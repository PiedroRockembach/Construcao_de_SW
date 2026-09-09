package br.pucrs.construcao.pecas.repository;

import br.pucrs.construcao.pecas.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PecaRepository extends JpaRepository<Peca, Long> {

    Optional<Peca> findByNumeroIdentificacao(String numeroIdentificacao);

    List<Peca> findByNomeContainingIgnoreCase(String nome);

    boolean existsByNumeroIdentificacao(String numeroIdentificacao);
}
