package br.pucrs.construcao.pecas.service;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.model.Peca;
import br.pucrs.construcao.pecas.repository.PecaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PecaService {

    private final PecaRepository pecaRepository;
    private final Counter pecasCreatedCounter;

    public PecaService(PecaRepository pecaRepository, MeterRegistry meterRegistry) {
        this.pecaRepository = pecaRepository;
        this.pecasCreatedCounter = Counter.builder("pecas.created.total")
                .description("Total de peças cadastradas com sucesso")
                .register(meterRegistry);
    }

    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        if (pecaRepository.existsByNumeroIdentificacao(dto.getNumeroIdentificacao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma peça cadastrada com o número de identificação: " + dto.getNumeroIdentificacao());
        }

        Peca peca = new Peca(
                dto.getNumeroIdentificacao(),
                dto.getNome(),
                dto.getDescricao()
        );
        Peca salva = pecaRepository.save(peca);
        pecasCreatedCounter.increment();
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Peça não encontrada com ID: " + id));
        return PecaResponseDTO.fromEntity(peca);
    }

    public PecaResponseDTO buscarPorNumeroIdentificacao(String numeroIdentificacao) {
        Peca peca = pecaRepository.findByNumeroIdentificacao(numeroIdentificacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Peça não encontrada com número de identificação: " + numeroIdentificacao));
        return PecaResponseDTO.fromEntity(peca);
    }

    public List<PecaResponseDTO> buscarPorNome(String nome) {
        return pecaRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(PecaResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
