package br.pucrs.construcao.clientes.persistence;

import br.pucrs.construcao.clientes.model.Cliente;
import br.pucrs.construcao.clientes.repository.ClienteRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementação em memória de persistência para ClienteRepository.
 * Isola 100% o framework de persistência (Spring Data JPA / Hibernate) e o banco de dados.
 */
public class InMemoryClienteRepository implements ClienteRepository {

    private final Map<Long, Cliente> database = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Cliente> findByCpf(String cpf) {
        if (cpf == null) return Optional.empty();
        return database.values().stream()
                .filter(c -> cpf.equalsIgnoreCase(c.getCpf()))
                .findFirst();
    }

    @Override
    public List<Cliente> findByNomeContainingIgnoreCase(String nome) {
        if (nome == null || nome.isBlank()) {
            return new ArrayList<>(database.values());
        }
        String lower = nome.toLowerCase();
        return database.values().stream()
                .filter(c -> c.getNome() != null && c.getNome().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCpf(String cpf) {
        if (cpf == null) return false;
        return database.values().stream()
                .anyMatch(c -> cpf.equalsIgnoreCase(c.getCpf()));
    }

    @Override
    public <S extends Cliente> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.incrementAndGet());
        }
        database.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(database.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        return database.containsKey(id);
    }

    @Override
    public List<Cliente> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public List<Cliente> findAllById(Iterable<Long> ids) {
        List<Cliente> result = new ArrayList<>();
        for (Long id : ids) {
            Cliente c = database.get(id);
            if (c != null) result.add(c);
        }
        return result;
    }

    @Override
    public long count() {
        return database.size();
    }

    @Override
    public void deleteById(Long id) {
        if (id != null) database.remove(id);
    }

    @Override
    public void delete(Cliente entity) {
        if (entity != null && entity.getId() != null) {
            database.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        for (Long id : ids) {
            if (id != null) database.remove(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends Cliente> entities) {
        for (Cliente entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        database.clear();
    }

    @Override
    public <S extends Cliente> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S entity : entities) {
            list.add(save(entity));
        }
        return list;
    }

    @Override
    public void flush() {
    }

    @Override
    public <S extends Cliente> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends Cliente> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<Cliente> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {
        deleteAllById(longs);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public Cliente getOne(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public Cliente getById(Long id) {
        return getOne(id);
    }

    @Override
    public Cliente getReferenceById(Long id) {
        return getOne(id);
    }

    @Override
    public <S extends Cliente> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente> long count(Example<S> example) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public <S extends Cliente, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("QueryByExample não suportado na implementação in-memory");
    }

    @Override
    public List<Cliente> findAll(Sort sort) {
        return findAll();
    }

    @Override
    public Page<Cliente> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Paginação não suportada na implementação in-memory");
    }
}
