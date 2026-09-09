package br.pucrs.construcao.representantes.controller;

import br.pucrs.construcao.representantes.dto.RepresentanteRequestDTO;
import br.pucrs.construcao.representantes.dto.RepresentanteResponseDTO;
import br.pucrs.construcao.representantes.service.RepresentanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/representantes")
public class RepresentanteController {

    private final RepresentanteService representanteService;

    public RepresentanteController(RepresentanteService representanteService) {
        this.representanteService = representanteService;
    }

    @PostMapping
    public ResponseEntity<RepresentanteResponseDTO> cadastrar(@Valid @RequestBody RepresentanteRequestDTO dto) {
        RepresentanteResponseDTO criado = representanteService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<RepresentanteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(representanteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepresentanteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.buscarPorId(id));
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<RepresentanteResponseDTO> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(representanteService.buscarPorCpf(cpf));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<RepresentanteResponseDTO>> buscarPorNome(@RequestParam(name = "nome", defaultValue = "") String nome) {
        return ResponseEntity.ok(representanteService.buscarPorNome(nome));
    }
}
