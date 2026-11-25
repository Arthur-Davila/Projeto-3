package com.wxn.backend.fila.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Chatbot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false)
	private boolean ativo;

	protected Chatbot() {
	}

	public Chatbot(String nome) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Nome do chatbot é obrigatório");
		}
		this.nome = nome;
		this.ativo = true;
	}

	public void transferirParaFila(Conversa conversa, String assunto, String equipeDestino) {
		if (!ativo) {
			throw new TransicaoInvalidaException("Chatbot inativo não pode transferir conversas");
		}
		if (conversa == null) {
			throw new IllegalArgumentException("Conversa é obrigatória");
		}
		conversa.definirTriagem(assunto, equipeDestino);
		conversa.encaminharParaHumano();
	}

	public void ativar() {
		this.ativo = true;
	}

	public void desativar() {
		this.ativo = false;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public boolean isAtivo() {
		return ativo;
	}
}