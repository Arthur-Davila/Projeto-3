package com.wxn.backend.fila.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class TicketAtendimento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String protocolo;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversa_id", nullable = false, unique = true, updatable = false)
	private Conversa conversa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "atendente_id", nullable = false)
	private Atendente atendente;

	@Column(nullable = false, updatable = false)
	private LocalDateTime abertoEm;

	private LocalDateTime fechadoEm;

	private String resolucao;

	protected TicketAtendimento() {
	}

	public TicketAtendimento(String protocolo, Conversa conversa, Atendente atendente) {
		if (protocolo == null || protocolo.isBlank()) {
			throw new IllegalArgumentException("Protocolo do ticket é obrigatório");
		}
		if (conversa == null) {
			throw new IllegalArgumentException("Conversa é obrigatória");
		}
		if (atendente == null) {
			throw new IllegalArgumentException("Atendente é obrigatório");
		}
		this.protocolo = protocolo;
		this.conversa = conversa;
		this.atendente = atendente;
		this.abertoEm = LocalDateTime.now();
	}

	public void concluir(String resolucao) {
		if (this.fechadoEm != null) {
			throw new TransicaoInvalidaException("Ticket já está fechado");
		}
		if (resolucao == null || resolucao.isBlank()) {
			throw new IllegalArgumentException("Descrição da resolução é obrigatória");
		}
		this.resolucao = resolucao;
		this.fechadoEm = LocalDateTime.now();
		this.conversa.encerrar();
	}

	public boolean isAberto() {
		return fechadoEm == null;
	}

	public Long getId() {
		return id;
	}

	public String getProtocolo() {
		return protocolo;
	}

	public Conversa getConversa() {
		return conversa;
	}

	public Atendente getAtendente() {
		return atendente;
	}

	public LocalDateTime getAbertoEm() {
		return abertoEm;
	}

	public LocalDateTime getFechadoEm() {
		return fechadoEm;
	}

	public String getResolucao() {
		return resolucao;
	}
}