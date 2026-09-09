package br.pucrs.construcao.pecas.dto;

import br.pucrs.construcao.pecas.model.Peca;

public class PecaResponseDTO {

    private Long id;
    private String numeroIdentificacao;
    private String nome;
    private String descricao;

    public PecaResponseDTO() {
    }

    public PecaResponseDTO(Long id, String numeroIdentificacao, String nome, String descricao) {
        this.id = id;
        this.numeroIdentificacao = numeroIdentificacao;
        this.nome = nome;
        this.descricao = descricao;
    }

    public static PecaResponseDTO fromEntity(Peca peca) {
        return new PecaResponseDTO(
                peca.getId(),
                peca.getNumeroIdentificacao(),
                peca.getNome(),
                peca.getDescricao()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
