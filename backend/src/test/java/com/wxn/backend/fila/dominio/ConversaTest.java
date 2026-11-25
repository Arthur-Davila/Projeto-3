package com.wxn.backend.fila.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

// JUnit puro: o domínio não depende de Spring nem de banco, então testamos sem subir contexto.
class ConversaTest {

	private static final String TELEFONE = "5581999990000";

	private Cliente clientePadrao() {
		return new Cliente("Maria Silva", TELEFONE);
	}

	private Atendente atendentePadrao() {
		return new Atendente("Carlos Souza", "carlos@wxn.com", "Suporte");
	}

	private Conversa conversaNaFila() {
		Conversa conversa = new Conversa(clientePadrao());
		conversa.encaminharParaHumano();
		return conversa;
	}

	// --- Criação ---

	@Test
	void conversaNovaNasceEmAtendimentoPeloBot() {
		Cliente cliente = clientePadrao();
		Conversa conversa = new Conversa(cliente);

		assertEquals(StatusConversa.EM_ATENDIMENTO_BOT, conversa.getStatus());
		assertSame(cliente, conversa.getCliente());
		assertEquals(TELEFONE, conversa.getTelefoneCliente());
		assertNull(conversa.getEntrouNaFilaEm());
		assertNull(conversa.getAtendente());
		assertTrue(conversa.getMensagens().isEmpty());
	}

	@Test
	void naoPermiteCriarConversaSemCliente() {
		assertThrows(IllegalArgumentException.class, () -> new Conversa(null));
	}

	// --- Transições válidas ---

	@Test
	void encaminharParaHumanoColocaNaFilaERegistraHorario() {
		LocalDateTime antes = LocalDateTime.now();
		Conversa conversa = conversaNaFila();

		assertEquals(StatusConversa.AGUARDANDO_HUMANO, conversa.getStatus());
		assertNotNull(conversa.getEntrouNaFilaEm());
		assertFalse(conversa.getEntrouNaFilaEm().isBefore(antes));
	}

	@Test
	void assumirPorAtendenteMudaStatusEGuardaAtendente() {
		Conversa conversa = conversaNaFila();
		Atendente atendente = atendentePadrao();

		conversa.assumirPor(atendente);

		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, conversa.getStatus());
		assertSame(atendente, conversa.getAtendente());
	}

	@Test
	void podeEncerrarConversaEmAtendimentoPeloBot() {
		Conversa conversa = new Conversa(clientePadrao());

		conversa.encerrar();

		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
	}

	@Test
	void podeEncerrarConversaAguardandoHumano() {
		Conversa conversa = conversaNaFila();

		conversa.encerrar();

		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
	}

	@Test
	void podeEncerrarConversaEmAtendimentoHumano() {
		Conversa conversa = conversaNaFila();
		conversa.assumirPor(atendentePadrao());

		conversa.encerrar();

		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
	}

	// --- Transições inválidas ---

	@Test
	void naoPodeEncaminharParaHumanoQuandoJaEstaNaFila() {
		Conversa conversa = conversaNaFila();

		assertThrows(TransicaoInvalidaException.class, conversa::encaminharParaHumano);
	}

	@Test
	void naoPodeEncaminharParaHumanoQuandoJaEstaComAtendente() {
		Conversa conversa = conversaNaFila();
		conversa.assumirPor(atendentePadrao());

		assertThrows(TransicaoInvalidaException.class, conversa::encaminharParaHumano);
	}

	@Test
	void naoPodeEncaminharParaHumanoConversaEncerrada() {
		Conversa conversa = new Conversa(clientePadrao());
		conversa.encerrar();

		assertThrows(TransicaoInvalidaException.class, conversa::encaminharParaHumano);
	}

	@Test
	void naoPodeAssumirConversaQueAindaEstaComOBot() {
		Conversa conversa = new Conversa(clientePadrao());
		Atendente atendente = atendentePadrao();

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(atendente));
	}

	@Test
	void naoPodeAssumirConversaJaAssumidaPorOutroAtendente() {
		Conversa conversa = conversaNaFila();
		Atendente primeiro = atendentePadrao();
		Atendente segundo = new Atendente("Ana Lima", "ana@wxn.com", "Suporte");
		conversa.assumirPor(primeiro);

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(segundo));
		assertSame(primeiro, conversa.getAtendente());
	}

	@Test
	void naoPodeAssumirConversaComAtendenteIndisponivel() {
		Conversa conversa = conversaNaFila();
		Atendente atendente = atendentePadrao();
		atendente.ficarIndisponivel();

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(atendente));
	}

	@Test
	void naoPodeAssumirConversaEncerrada() {
		Conversa conversa = conversaNaFila();
		conversa.encerrar();
		Atendente atendente = atendentePadrao();

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(atendente));
	}

	@Test
	void naoPodeAssumirSemInformarAtendente() {
		Conversa conversa = conversaNaFila();

		assertThrows(IllegalArgumentException.class, () -> conversa.assumirPor(null));
		assertEquals(StatusConversa.AGUARDANDO_HUMANO, conversa.getStatus());
	}

	@Test
	void naoPodeEncerrarConversaJaEncerrada() {
		Conversa conversa = new Conversa(clientePadrao());
		conversa.encerrar();

		assertThrows(TransicaoInvalidaException.class, conversa::encerrar);
	}

	// --- podeSerAssumida ---

	@Test
	void soPodeSerAssumidaQuandoEstaAguardandoHumano() {
		Conversa conversa = new Conversa(clientePadrao());
		assertFalse(conversa.podeSerAssumida());

		conversa.encaminharParaHumano();
		assertTrue(conversa.podeSerAssumida());

		conversa.assumirPor(atendentePadrao());
		assertFalse(conversa.podeSerAssumida());

		conversa.encerrar();
		assertFalse(conversa.podeSerAssumida());
	}

	// --- tempoDeEspera ---

	@Test
	void tempoDeEsperaEhZeroEnquantoBotAtende() {
		Conversa conversa = new Conversa(clientePadrao());

		assertEquals(Duration.ZERO, conversa.tempoDeEspera(LocalDateTime.now()));
	}

	@Test
	void tempoDeEsperaContaDesdeQueEntrouNaFila() {
		Conversa conversa = conversaNaFila();
		LocalDateTime agora = conversa.getEntrouNaFilaEm().plusMinutes(7);

		assertEquals(Duration.ofMinutes(7), conversa.tempoDeEspera(agora));
	}

	@Test
	void tempoDeEsperaEhZeroDepoisDeAssumida() {
		Conversa conversa = conversaNaFila();
		LocalDateTime agora = conversa.getEntrouNaFilaEm().plusMinutes(7);
		conversa.assumirPor(atendentePadrao());

		assertEquals(Duration.ZERO, conversa.tempoDeEspera(agora));
	}

	@Test
	void tempoDeEsperaEhZeroDepoisDeEncerrada() {
		Conversa conversa = conversaNaFila();
		LocalDateTime agora = conversa.getEntrouNaFilaEm().plusMinutes(7);
		conversa.encerrar();

		assertEquals(Duration.ZERO, conversa.tempoDeEspera(agora));
	}

	// --- Mensagens ---

	@Test
	void adicionarMensagemVinculaAConversaEGuardaDados() {
		Conversa conversa = new Conversa(clientePadrao());

		Mensagem mensagem = conversa.adicionarMensagem("Olá", OrigemMensagem.CLIENTE);

		assertSame(conversa, mensagem.getConversa());
		assertEquals("Olá", mensagem.getConteudo());
		assertEquals(OrigemMensagem.CLIENTE, mensagem.getOrigem());
		assertNotNull(mensagem.getEnviadaEm());
		assertEquals(List.of(mensagem), conversa.getMensagens());
	}

	@Test
	void mensagensMantemOrdemDeInsercao() {
		Conversa conversa = new Conversa(clientePadrao());

		Mensagem primeira = conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);
		Mensagem segunda = conversa.adicionarMensagem("Como posso ajudar?", OrigemMensagem.BOT);

		assertEquals(List.of(primeira, segunda), conversa.getMensagens());
	}

	@Test
	void naoPermiteMensagemSemConteudoOuSemOrigem() {
		Conversa conversa = new Conversa(clientePadrao());

		assertThrows(IllegalArgumentException.class,
				() -> conversa.adicionarMensagem("  ", OrigemMensagem.CLIENTE));
		assertThrows(IllegalArgumentException.class,
				() -> conversa.adicionarMensagem("Oi", null));
		assertTrue(conversa.getMensagens().isEmpty());
	}

	@Test
	void listaDeMensagensNaoPodeSerModificadaPorFora() {
		Conversa conversa = new Conversa(clientePadrao());
		Mensagem mensagem = conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);
		List<Mensagem> lista = conversa.getMensagens();

		assertThrows(UnsupportedOperationException.class, () -> lista.add(mensagem));
		assertThrows(UnsupportedOperationException.class, lista::clear);
		assertEquals(1, conversa.getMensagens().size());
	}

	@Test
	void listaDevolvidaNaoRefleteMensagensAdicionadasDepois() {
		Conversa conversa = new Conversa(clientePadrao());
		List<Mensagem> copia = conversa.getMensagens();

		conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);

		assertTrue(copia.isEmpty());
	}

}