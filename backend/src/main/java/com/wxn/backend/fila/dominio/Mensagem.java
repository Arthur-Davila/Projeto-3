package com.wxn.backend.fila.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Mensagem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// updatable=false: mensagem é histórico, nunca é editada depois de enviada.
	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	private String conteudo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private OrigemMensagem origem;

	@Column(nullable = false, updatable = false)
	private LocalDateTime enviadaEm;

	// LAZY: ao listar mensagens não precisamos recarregar a conversa a cada linha.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversa_id", nullable = false, updatable = false)
	private Conversa conversa;

	// Exigido pelo JPA; protected para que o código de negócio não crie mensagens vazias.
	protected Mensagem() {
	}

	// Visibilidade de pacote: só a Conversa (raiz do agregado) cria mensagens,
	// garantindo que os dois lados da associação fiquem sempre consistentes.
	Mensagem(Conversa conversa, String conteudo, OrigemMensagem origem) {
		this.conversa = conversa;
		this.conteudo = conteudo;
		this.origem = origem;
		this.enviadaEm = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getConteudo() {
		return conteudo;
	}

	public OrigemMensagem getOrigem() {
		return origem;
	}

	public LocalDateTime getEnviadaEm() {
		return enviadaEm;
	}

	public Conversa getConversa() {
		return conversa;
	}

}
