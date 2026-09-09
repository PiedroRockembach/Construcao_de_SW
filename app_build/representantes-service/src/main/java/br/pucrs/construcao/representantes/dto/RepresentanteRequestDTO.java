package br.pucrs.construcao.representantes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RepresentanteRequestDTO {

    @NotBlank(message = "O CPF do representante é obrigatório.")
    @Pattern(regexp = "(^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$)|(^\\d{11}$)", 
             message = "O CPF deve estar no formato 000.000.000-00 ou conter 11 dígitos numéricos.")
    private String cpf;

    @NotBlank(message = "O nome do representante é obrigatório.")
    @Size(min = 2, max = 150, message = "O nome do representante deve ter entre 2 e 150 caracteres.")
    private String nome;

    public RepresentanteRequestDTO() {
    }

    public RepresentanteRequestDTO(String cpf, String nome) {
        this.cpf = cpf;
        this.nome = nome;
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
