package br.pucrs.construcao.pecas.service;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.model.Peca;
import br.pucrs.construcao.pecas.repository.PecaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PecaService {

    private final PecaRepository pecaRepository;
    private final Counter cadastroSucessoCounter;
    private final Counter cadastroConflitoCounter;
    private final Counter consultaNaoEncontradaCounter;

    public PecaService(PecaRepository pecaRepository, MeterRegistry meterRegistry) {
        this.pecaRepository = pecaRepository;
        this.cadastroSucessoCounter = Counter.builder("pecas.cadastro")
                .description("Tentativas de cadastro de peças")
                .tag("resultado", "sucesso")
                .register(meterRegistry);
        this.cadastroConflitoCounter = Counter.builder("pecas.cadastro")
                .description("Tentativas de cadastro de peças")
                .tag("resultado", "conflito")
                .register(meterRegistry);
        this.consultaNaoEncontradaCounter = Counter.builder("pecas.consulta.nao_encontrada")
                .description("Consultas de peças que retornaram 404")
                .register(meterRegistry);
        Gauge.builder("pecas.registros", pecaRepository, r -> r.count())
                .description("Quantidade atual de peças na base")
                .register(meterRegistry);
    }

    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        if (pecaRepository.existsByNumeroIdentificacao(dto.getNumeroIdentificacao())) {
            cadastroConflitoCounter.increment();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma peça cadastrada com o número de identificação: " + dto.getNumeroIdentificacao());
        }

        Peca peca = new Peca(
                dto.getNumeroIdentificacao(),
                dto.getNome(),
                dto.getDescricao()
        );
        Peca salva = pecaRepository.save(peca);
        cadastroSucessoCounter.increment();
        return PecaResponseDTO.fromEntity(salva);
    }

    public List<PecaResponseDTO> listarTodas() {
        return pecaRepository.findAll()
                .stream()
                .map(PecaResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PecaResponseDTO buscarPorId(Long id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Peça não encontrada com ID: " + id);
                });
        return PecaResponseDTO.fromEntity(peca);
    }

    public PecaResponseDTO buscarPorNumeroIdentificacao(String numeroIdentificacao) {
        Peca peca = pecaRepository.findByNumeroIdentificacao(numeroIdentificacao)
                .orElseThrow(() -> {
                    consultaNaoEncontradaCounter.increment();
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Peça não encontrada com número de identificação: " + numeroIdentificacao);
                });
        return PecaResponseDTO.fromEntity(peca);
    }

    public List<PecaResponseDTO> buscarPorNome(String nome) {
        return pecaRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(PecaResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
