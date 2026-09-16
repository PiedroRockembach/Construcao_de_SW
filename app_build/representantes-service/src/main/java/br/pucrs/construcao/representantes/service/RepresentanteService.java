package br.pucrs.construcao.representantes.service;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.model.Representante;
import br.pucrs.construcao.representantes.repository.RepresentanteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepresentanteService {

    private final RepresentanteRepository representanteRepository;
    private final Counter cadastroSucessoCounter;
    private final Counter cadastroConflitoCounter;
    private final Counter consultaNaoEncontradaCounter;

    public RepresentanteService(RepresentanteRepository representanteRepository, MeterRegistry meterRegistry) {
        this.representanteRepository = representanteRepository;
        this.cadastroSucessoCounter = Counter.builder("representantes.cadastro")
                .description("Tentativas de cadastro de representantes")
                .tag("resultado", "sucesso")
                .register(meterRegistry);
        this.cadastroConflitoCounter = Counter.builder("representantes.cadastro")
                .description("Tentativas de cadastro de representantes")
                .tag("resultado", "conflito")
                .register(meterRegistry);
        this.consultaNaoEncontradaCounter = Counter.builder("representantes.consulta.nao_encontrada")
                .description("Consultas de representantes que retornaram 404")
                .register(meterRegistry);
        Gauge.builder("representantes.registros", representanteRepository, r -> r.count())
                .description("Quantidade atual de representantes na base")
                .register(meterRegistry);
    }

    public RepresentanteResponseDTO cadastrar(RepresentanteRequestDTO dto) {
        String cpfLimpo = dto.getCpf().trim();
        if (representanteRepository.existsByCpf(cpfLimpo)) {
            cadastroConflitoCounter.increment();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um representante cadastrado com o CPF: " + cpfLimpo);
        }

        Representante representante = new Representante(cpfLimpo, dto.getNome().trim());
        Representante salvo = representanteRepository.save(representante);
        cadastroSucessoCounter.increment();
        return RepresentanteResponseDTO.fromEntity(salvo);
    }

    public List<RepresentanteResponseDTO> listarTodos() {
        return representanteRepository.findAll()
                .stream()
                .map(RepresentanteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public RepresentanteResponseDTO buscarPorId(Long id) {
        Representante representante = representanteRepository.findById(id)
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Representante não encontrado com o ID: " + id);
                });
        return RepresentanteResponseDTO.fromEntity(representante);
    }

    public RepresentanteResponseDTO buscarPorCpf(String cpf) {
        Representante representante = representanteRepository.findByCpf(cpf.trim())
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Representante não encontrado com o CPF: " + cpf);
                });
        return RepresentanteResponseDTO.fromEntity(representante);
    }

    public List<RepresentanteResponseDTO> buscarPorNome(String nome) {
        return representanteRepository.findByNomeContainingIgnoreCase(nome.trim())
                .stream()
                .map(RepresentanteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
