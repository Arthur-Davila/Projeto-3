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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

@Entity
public class Conversa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// O cliente não muda durante a conversa; updatable=false protege isso também no banco.
	@Column(nullable = false, updatable = false)
	private String telefoneCliente;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusConversa status;

	// Nulo enquanto o bot atende: só faz sentido medir espera depois de entrar na fila.
	private LocalDateTime entrouNaFilaEm;

	// Só o id, para não acoplar este agregado a uma entidade de usuário que ainda não existe.
	private Long atendenteId;

	// Optimistic locking: se dois atendentes assumirem a mesma conversa ao mesmo tempo,
	// o segundo commit falha (OptimisticLockException) em vez de sobrescrever o primeiro.
	@Version
	private Long versao;

	// @OrderBy garante a ordem cronológica também quando a lista vem do banco.
	@OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("enviadaEm ASC")
	private List<Mensagem> mensagens = new ArrayList<>();

	// Exigido pelo JPA; protected para impedir conversas sem telefone no código de negócio.
	protected Conversa() {
	}

	public Conversa(String telefoneCliente) {
		if (telefoneCliente == null || telefoneCliente.isBlank()) {
			throw new IllegalArgumentException("Telefone do cliente é obrigatório");
		}
		this.telefoneCliente = telefoneCliente;
		this.status = StatusConversa.EM_ATENDIMENTO_BOT;
	}

	// Para repositórios que não geram id sozinhos (ex.: em memória); com JPA o banco preenche.
	// Aceita uma única atribuição: depois de salva, a conversa nunca troca de identidade.
	public void atribuirId(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Id é obrigatório");
		}
		if (this.id != null) {
			throw new IllegalStateException("Conversa já possui id " + this.id);
		}
		this.id = id;
	}

	// --- Ações: única forma de mudar o status, sempre validando a transição ---

	public void encaminharParaHumano() {
		exigirStatus(StatusConversa.EM_ATENDIMENTO_BOT, "encaminhar para humano");
		this.status = StatusConversa.AGUARDANDO_HUMANO;
		this.entrouNaFilaEm = LocalDateTime.now();
	}

	public void assumirPor(Long atendenteId) {
		// Validação do argumento antes da transição, para não deixar estado pela metade.
		if (atendenteId == null) {
			throw new IllegalArgumentException("Id do atendente é obrigatório");
		}
		exigirStatus(StatusConversa.AGUARDANDO_HUMANO, "assumir");
		this.status = StatusConversa.EM_ATENDIMENTO_HUMANO;
		this.atendenteId = atendenteId;
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

	// Recebe "agora" por parâmetro para o cálculo ser determinístico e testável sem relógio real.
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

	public String getTelefoneCliente() {
		return telefoneCliente;
	}

	public StatusConversa getStatus() {
		return status;
	}

	public LocalDateTime getEntrouNaFilaEm() {
		return entrouNaFilaEm;
	}

	public Long getAtendenteId() {
		return atendenteId;
	}

	public Long getVersao() {
		return versao;
	}

	// Cópia imutável: de fora não dá para adicionar/remover mensagens sem passar pela regra.
	public List<Mensagem> getMensagens() {
		return List.copyOf(mensagens);
	}

}
