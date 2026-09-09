package br.pucrs.construcao.representantes.repository;

import br.pucrs.construcao.representantes.model.Representante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepresentanteRepository extends JpaRepository<Representante, Long> {

    Optional<Representante> findByCpf(String cpf);

    List<Representante> findByNomeContainingIgnoreCase(String nome);

    boolean existsByCpf(String cpf);
}
