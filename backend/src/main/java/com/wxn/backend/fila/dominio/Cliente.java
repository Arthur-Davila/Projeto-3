package com.wxn.backend.fila.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, unique = true)
	private String telefone;

	@Column(columnDefinition = "TEXT")
	private String dadosInformados;

	protected Cliente() {
	}

	public Cliente(String nome, String telefone) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Nome do cliente é obrigatório");
		}
		if (telefone == null || telefone.isBlank()) {
			throw new IllegalArgumentException("Telefone do cliente é obrigatório");
		}
		this.nome = nome;
		this.telefone = telefone;
	}

	public void registrarDadosTriagem(String dadosInformados) {
		if (dadosInformados == null || dadosInformados.isBlank()) {
			throw new IllegalArgumentException("Dados da triagem não podem ser vazios");
		}
		this.dadosInformados = dadosInformados;
	}

	public void atualizarNome(String novoNome) {
		if (novoNome == null || novoNome.isBlank()) {
			throw new IllegalArgumentException("Nome do cliente é obrigatório");
		}
		this.nome = novoNome;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getDadosInformados() {
		return dadosInformados;
	}
}