package com.wxn.backend.fila.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Atendente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String equipe;

	@Column(nullable = false)
	private boolean disponivel;

	protected Atendente() {
	}

	public Atendente(String nome, String email, String equipe) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Nome do atendente é obrigatório");
		}
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("E-mail do atendente é obrigatório");
		}
		if (equipe == null || equipe.isBlank()) {
			throw new IllegalArgumentException("Equipe do atendente é obrigatória");
		}
		this.nome = nome;
		this.email = email;
		this.equipe = equipe;
		this.disponivel = true;
	}

	public void ficarIndisponivel() {
		this.disponivel = false;
	}

	public void ficarDisponivel() {
		this.disponivel = true;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getEquipe() {
		return equipe;
	}

	public boolean isDisponivel() {
		return disponivel;
	}
}