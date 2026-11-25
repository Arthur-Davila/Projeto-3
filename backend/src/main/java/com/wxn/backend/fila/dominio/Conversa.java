package com.wxn.backend.fila.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

@Entity
public class Conversa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false, updatable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chatbot_id")
	private Chatbot chatbot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusConversa status;

	private String assunto;

	private String equipe;

	private LocalDateTime entrouNaFilaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id")
	private Atendente atendente;

	@Version
	private Long versao;

	@OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("enviadaEm ASC")
	private List<Mensagem> mensagens = new ArrayList<>();

	protected Conversa() {
	}

	public Conversa(Cliente cliente) {
		if (cliente == null) {
			throw new IllegalArgumentException("Cliente é obrigatório");
		}
		this.cliente = cliente;
		this.status = StatusConversa.EM_ATENDIMENTO_BOT;
	}

	public Conversa(Cliente cliente, Chatbot chatbot) {
		this(cliente);
		this.chatbot = chatbot;
	}

	// --- Ações: única forma de mudar o estado, sempre validando a transição ---

	public void definirTriagem(String assunto, String equipe) {
		if (assunto != null && !assunto.isBlank()) {
			this.assunto = assunto;
		}
		if (equipe != null && !equipe.isBlank()) {
			this.equipe = equipe;
		}
	}

	public void encaminharParaHumano() {
		exigirStatus(StatusConversa.EM_ATENDIMENTO_BOT, "encaminhar para humano");
		this.status = StatusConversa.AGUARDANDO_HUMANO;
		this.entrouNaFilaEm = LocalDateTime.now();
	}

	public void assumirPor(Atendente atendente) {
		if (atendente == null) {
			throw new IllegalArgumentException("Atendente é obrigatório");
		}
		if (!atendente.isDisponivel()) {
			throw new TransicaoInvalidaException("Atendente não está disponível");
		}
		exigirStatus(StatusConversa.AGUARDANDO_HUMANO, "assumir");
		this.status = StatusConversa.EM_ATENDIMENTO_HUMANO;
		this.atendente = atendente;
	}

	public void encerrar() {
		if (status == StatusConversa.ENCERRADA) {
			throw new TransicaoInvalidaException("Conversa já está encerrada");
		}
		this.status = StatusConversa.ENCERRADA;
	}

	public boolean podeSerAssumida() {
		return status == StatusConversa.AGUARDANDO_HUMANO;
	}

	public Duration tempoDeEspera(LocalDateTime agora) {
		if (status != StatusConversa.AGUARDANDO_HUMANO) {
			return Duration.ZERO;
		}
		return Duration.between(entrouNaFilaEm, agora);
	}

	public Mensagem adicionarMensagem(String conteudo, OrigemMensagem origem) {
		if (conteudo == null || conteudo.isBlank()) {
			throw new IllegalArgumentException("Conteúdo da mensagem é obrigatório");
		}
		if (origem == null) {
			throw new IllegalArgumentException("Origem da mensagem é obrigatória");
		}
		Mensagem mensagem = new Mensagem(this, conteudo, origem);
		mensagens.add(mensagem);
		return mensagem;
	}

	private void exigirStatus(StatusConversa esperado, String acao) {
		if (status != esperado) {
			throw new TransicaoInvalidaException(
					"Não é possível " + acao + " uma conversa com status " + status);
		}
	}

	// --- Getters (sem setters: o estado só muda pelas ações acima) ---

	public Long getId() {
		return id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Chatbot getChatbot() {
		return chatbot;
	}

	public String getTelefoneCliente() {
		return cliente != null ? cliente.getTelefone() : null;
	}

	public StatusConversa getStatus() {
		return status;
	}

	public String getAssunto() {
		return assunto;
	}

	public String getEquipe() {
		return equipe;
	}

	public LocalDateTime getEntrouNaFilaEm() {
		return entrouNaFilaEm;
	}

	public Atendente getAtendente() {
		return atendente;
	}

	public Long getAtendenteId() {
		return atendente != null ? atendente.getId() : null;
	}

	public Long getVersao() {
		return versao;
	}

	public List<Mensagem> getMensagens() {
		return List.copyOf(mensagens);
	}
}