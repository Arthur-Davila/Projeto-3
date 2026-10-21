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

	private Conversa conversaNaFila() {
		Conversa conversa = new Conversa(TELEFONE);
		conversa.encaminharParaHumano();
		return conversa;
	}

	// --- Criação ---

	@Test
	void conversaNovaNasceEmAtendimentoPeloBot() {
		Conversa conversa = new Conversa(TELEFONE);

		assertEquals(StatusConversa.EM_ATENDIMENTO_BOT, conversa.getStatus());
		assertEquals(TELEFONE, conversa.getTelefoneCliente());
		assertNull(conversa.getEntrouNaFilaEm());
		assertNull(conversa.getAtendenteId());
		assertTrue(conversa.getMensagens().isEmpty());
	}

	@Test
	void naoPermiteCriarConversaSemTelefone() {
		assertThrows(IllegalArgumentException.class, () -> new Conversa(null));
		assertThrows(IllegalArgumentException.class, () -> new Conversa("  "));
	}

	// --- Identidade ---

	@Test
	void atribuirIdDefineOIdUmaUnicaVez() {
		Conversa conversa = new Conversa(TELEFONE);

		conversa.atribuirId(10L);

		assertEquals(10L, conversa.getId());
		assertThrows(IllegalStateException.class, () -> conversa.atribuirId(11L));
		assertEquals(10L, conversa.getId());
	}

	@Test
	void naoPermiteAtribuirIdNulo() {
		Conversa conversa = new Conversa(TELEFONE);

		assertThrows(IllegalArgumentException.class, () -> conversa.atribuirId(null));
		assertNull(conversa.getId());
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

		conversa.assumirPor(42L);

		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, conversa.getStatus());
		assertEquals(42L, conversa.getAtendenteId());
	}

	@Test
	void podeEncerrarConversaEmAtendimentoPeloBot() {
		Conversa conversa = new Conversa(TELEFONE);

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
		conversa.assumirPor(1L);

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
		conversa.assumirPor(1L);

		assertThrows(TransicaoInvalidaException.class, conversa::encaminharParaHumano);
	}

	@Test
	void naoPodeEncaminharParaHumanoConversaEncerrada() {
		Conversa conversa = new Conversa(TELEFONE);
		conversa.encerrar();

		assertThrows(TransicaoInvalidaException.class, conversa::encaminharParaHumano);
	}

	@Test
	void naoPodeAssumirConversaQueAindaEstaComOBot() {
		Conversa conversa = new Conversa(TELEFONE);

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(1L));
	}

	@Test
	void naoPodeAssumirConversaJaAssumidaPorOutroAtendente() {
		Conversa conversa = conversaNaFila();
		conversa.assumirPor(1L);

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(2L));
		assertEquals(1L, conversa.getAtendenteId());
	}

	@Test
	void naoPodeAssumirConversaEncerrada() {
		Conversa conversa = conversaNaFila();
		conversa.encerrar();

		assertThrows(TransicaoInvalidaException.class, () -> conversa.assumirPor(1L));
	}

	@Test
	void naoPodeAssumirSemInformarAtendente() {
		Conversa conversa = conversaNaFila();

		assertThrows(IllegalArgumentException.class, () -> conversa.assumirPor(null));
		assertEquals(StatusConversa.AGUARDANDO_HUMANO, conversa.getStatus());
	}

	@Test
	void naoPodeEncerrarConversaJaEncerrada() {
		Conversa conversa = new Conversa(TELEFONE);
		conversa.encerrar();

		assertThrows(TransicaoInvalidaException.class, conversa::encerrar);
	}

	// --- podeSerAssumida ---

	@Test
	void soPodeSerAssumidaQuandoEstaAguardandoHumano() {
		Conversa conversa = new Conversa(TELEFONE);
		assertFalse(conversa.podeSerAssumida());

		conversa.encaminharParaHumano();
		assertTrue(conversa.podeSerAssumida());

		conversa.assumirPor(1L);
		assertFalse(conversa.podeSerAssumida());

		conversa.encerrar();
		assertFalse(conversa.podeSerAssumida());
	}

	// --- tempoDeEspera ---

	@Test
	void tempoDeEsperaEhZeroEnquantoBotAtende() {
		Conversa conversa = new Conversa(TELEFONE);

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
		conversa.assumirPor(1L);

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
		Conversa conversa = new Conversa(TELEFONE);

		Mensagem mensagem = conversa.adicionarMensagem("Olá", OrigemMensagem.CLIENTE);

		assertSame(conversa, mensagem.getConversa());
		assertEquals("Olá", mensagem.getConteudo());
		assertEquals(OrigemMensagem.CLIENTE, mensagem.getOrigem());
		assertNotNull(mensagem.getEnviadaEm());
		assertEquals(List.of(mensagem), conversa.getMensagens());
	}

	@Test
	void mensagensMantemOrdemDeInsercao() {
		Conversa conversa = new Conversa(TELEFONE);

		Mensagem primeira = conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);
		Mensagem segunda = conversa.adicionarMensagem("Como posso ajudar?", OrigemMensagem.BOT);

		assertEquals(List.of(primeira, segunda), conversa.getMensagens());
	}

	@Test
	void naoPermiteMensagemSemConteudoOuSemOrigem() {
		Conversa conversa = new Conversa(TELEFONE);

		assertThrows(IllegalArgumentException.class,
				() -> conversa.adicionarMensagem("  ", OrigemMensagem.CLIENTE));
		assertThrows(IllegalArgumentException.class,
				() -> conversa.adicionarMensagem("Oi", null));
		assertTrue(conversa.getMensagens().isEmpty());
	}

	@Test
	void listaDeMensagensNaoPodeSerModificadaPorFora() {
		Conversa conversa = new Conversa(TELEFONE);
		Mensagem mensagem = conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);
		List<Mensagem> lista = conversa.getMensagens();

		assertThrows(UnsupportedOperationException.class, () -> lista.add(mensagem));
		assertThrows(UnsupportedOperationException.class, lista::clear);
		assertEquals(1, conversa.getMensagens().size());
	}

	@Test
	void listaDevolvidaNaoRefleteMensagensAdicionadasDepois() {
		Conversa conversa = new Conversa(TELEFONE);
		List<Mensagem> copia = conversa.getMensagens();

		conversa.adicionarMensagem("Oi", OrigemMensagem.CLIENTE);

		assertTrue(copia.isEmpty());
	}

}
