package br.pucrs.construcao.pecas.controller;

import br.pucrs.construcao.pecas.dto.PecaRequestDTO;
import br.pucrs.construcao.pecas.dto.PecaResponseDTO;
import br.pucrs.construcao.pecas.service.PecaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService pecaService;

    public PecaController(PecaService pecaService) {
        this.pecaService = pecaService;
    }

    @PostMapping
    public ResponseEntity<PecaResponseDTO> cadastrar(@Valid @RequestBody PecaRequestDTO dto) {
        PecaResponseDTO criada = pecaService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(pecaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<PecaResponseDTO>> buscarPorNome(@RequestParam(name = "nome", defaultValue = "") String nome) {
        return ResponseEntity.ok(pecaService.buscarPorNome(nome));
    }

    @GetMapping("/numero/{numeroIdentificacao}")
    public ResponseEntity<PecaResponseDTO> buscarPorNumero(@PathVariable String numeroIdentificacao) {
        return ResponseEntity.ok(pecaService.buscarPorNumeroIdentificacao(numeroIdentificacao));
    }
}
