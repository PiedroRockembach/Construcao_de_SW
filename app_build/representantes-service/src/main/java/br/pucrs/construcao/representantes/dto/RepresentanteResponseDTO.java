package br.pucrs.construcao.representantes.dto;

import br.pucrs.construcao.representantes.model.Representante;

public class RepresentanteResponseDTO {

    private Long id;
    private String cpf;
    private String nome;

    public RepresentanteResponseDTO() {
    }

    public RepresentanteResponseDTO(Long id, String cpf, String nome) {
        this.id = id;
        this.cpf = cpf;
        this.nome = nome;
    }

    public static RepresentanteResponseDTO fromEntity(Representante representante) {
        return new RepresentanteResponseDTO(
                representante.getId(),
                representante.getCpf(),
                representante.getNome()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
