package br.pucrs.construcao.clientes.service;

import br.pucrs.construcao.clientes.dto.ClienteRequestDTO;
import br.pucrs.construcao.clientes.dto.ClienteResponseDTO;
import br.pucrs.construcao.clientes.model.Cliente;
import br.pucrs.construcao.clientes.repository.ClienteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final Counter cadastroSucessoCounter;
    private final Counter cadastroConflitoCounter;
    private final Counter consultaNaoEncontradaCounter;

    public ClienteService(ClienteRepository clienteRepository, MeterRegistry meterRegistry) {
        this.clienteRepository = clienteRepository;
        this.cadastroSucessoCounter = Counter.builder("clientes.cadastro")
                .description("Tentativas de cadastro de clientes")
                .tag("resultado", "sucesso")
                .register(meterRegistry);
        this.cadastroConflitoCounter = Counter.builder("clientes.cadastro")
                .description("Tentativas de cadastro de clientes")
                .tag("resultado", "conflito")
                .register(meterRegistry);
        this.consultaNaoEncontradaCounter = Counter.builder("clientes.consulta.nao_encontrada")
                .description("Consultas de clientes que retornaram 404")
                .register(meterRegistry);
        Gauge.builder("clientes.registros", clienteRepository, r -> r.count())
                .description("Quantidade atual de clientes na base")
                .register(meterRegistry);
    }

    public ClienteResponseDTO cadastrar(ClienteRequestDTO dto) {
        String cpfLimpo = dto.getCpf().trim();
        if (clienteRepository.existsByCpf(cpfLimpo)) {
            cadastroConflitoCounter.increment();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um cliente cadastrado com o CPF: " + cpfLimpo);
        }

        Cliente cliente = new Cliente(cpfLimpo, dto.getNome().trim());
        Cliente salvo = clienteRepository.save(cliente);
        cadastroSucessoCounter.increment();
        return ClienteResponseDTO.fromEntity(salvo);
    }

    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Cliente não encontrado com o ID: " + id);
                });
        return ClienteResponseDTO.fromEntity(cliente);
    }

    public ClienteResponseDTO buscarPorCpf(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf.trim())
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Cliente não encontrado com o CPF: " + cpf);
                });
        return ClienteResponseDTO.fromEntity(cliente);
    }

    public List<ClienteResponseDTO> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome.trim())
                .stream()
                .map(ClienteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
