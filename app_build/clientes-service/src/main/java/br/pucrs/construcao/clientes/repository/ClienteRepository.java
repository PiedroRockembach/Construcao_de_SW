package br.pucrs.construcao.clientes.repository;

import br.pucrs.construcao.clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpf(String cpf);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    boolean existsByCpf(String cpf);
}
