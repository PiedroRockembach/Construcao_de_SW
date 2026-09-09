package br.pucrs.construcao.clientes.dto;

import br.pucrs.construcao.clientes.model.Cliente;

public class ClienteResponseDTO {

    private Long id;
    private String cpf;
    private String nome;

    public ClienteResponseDTO() {
    }

    public ClienteResponseDTO(Long id, String cpf, String nome) {
        this.id = id;
        this.cpf = cpf;
        this.nome = nome;
    }

    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getCpf(),
                cliente.getNome()
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
