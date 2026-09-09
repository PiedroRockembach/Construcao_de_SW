package br.pucrs.construcao.pecas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PecaRequestDTO {

    @NotBlank(message = "O número de identificação da peça é obrigatório.")
    @Size(max = 50, message = "O número de identificação deve ter no máximo 50 caracteres.")
    private String numeroIdentificacao;

    @NotBlank(message = "O nome da peça é obrigatório.")
    @Size(max = 150, message = "O nome da peça deve ter no máximo 150 caracteres.")
    private String nome;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
    private String descricao;

    public PecaRequestDTO() {
    }

    public PecaRequestDTO(String numeroIdentificacao, String nome, String descricao) {
        this.numeroIdentificacao = numeroIdentificacao;
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNumeroIdentificacao() {
        return numeroIdentificacao;
    }

    public void setNumeroIdentificacao(String numeroIdentificacao) {
        this.numeroIdentificacao = numeroIdentificacao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
