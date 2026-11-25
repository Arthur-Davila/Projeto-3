package com.wxn.backend.fila.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.Mensagem;
import com.wxn.backend.fila.dominio.OrigemMensagem;
import com.wxn.backend.fila.dominio.StatusConversa;

// Replace.NONE: usa o H2 de src/test/resources (modo PostgreSQL + Flyway), não um banco genérico.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConversaRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ConversaRepository repositorio;

	private Conversa conversaComBot(String telefone) {
		Cliente cliente = em.persist(new Cliente("Cliente " + telefone, telefone));
		return em.persist(new Conversa(cliente));
	}

	private Conversa conversaNaFila(String telefone) {
		Conversa conversa = conversaComBot(telefone);
		conversa.encaminharParaHumano();
		return conversa;
	}

	private List<Long> idsDaFila() {
		return repositorio.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO)
				.stream().map(Conversa::getId).toList();
	}

	// O banco guarda até microssegundos; 1 ms garante horários distintos depois de gravar.
	private void esperarRelogioAvancar(LocalDateTime referencia) {
		while (Duration.between(referencia, LocalDateTime.now()).toMillis() < 1) {
			Thread.onSpinWait();
		}
	}

	// --- Filtro ---

	@Test
	void filaTemSoConversasAguardandoHumano() {
		conversaComBot("5581000000001");
		Conversa naFila = conversaNaFila("5581000000002");
		Conversa assumida = conversaNaFila("5581000000003");
		assumida.assumirPor(em.persist(new Atendente("Ana Lima", "ana@wxn.com", "Suporte")));
		Conversa encerrada = conversaNaFila("5581000000004");
		encerrada.encerrar();
		em.flush();

		assertEquals(List.of(naFila.getId()), idsDaFila());
	}

	@Test
	void filaVaziaQuandoNinguemAguarda() {
		conversaComBot("5581000000001");
		em.flush();

		assertTrue(idsDaFila().isEmpty());
	}

	// --- Ordem ---

	@Test
	void ordenaPelaEntradaNaFilaENaoPeloId() {
		Conversa criadaPrimeiro = conversaComBot("5581000000001");
		Conversa criadaDepois = conversaComBot("5581000000002");

		criadaDepois.encaminharParaHumano();
		esperarRelogioAvancar(criadaDepois.getEntrouNaFilaEm());
		criadaPrimeiro.encaminharParaHumano();
		em.flush();

		assertEquals(List.of(criadaDepois.getId(), criadaPrimeiro.getId()), idsDaFila());
	}

	@Test
	void empateNoHorarioDeEntradaDesempataPeloId() {
		Conversa primeira = conversaNaFila("5581000000001");
		Conversa segunda = conversaNaFila("5581000000002");
		em.flush();
		// O domínio não permite escolher o horário de entrada, então o empate é forçado no banco.
		em.getEntityManager()
				.createQuery("update Conversa c set c.entrouNaFilaEm = :instante")
				.setParameter("instante", LocalDateTime.of(2026, 11, 24, 9, 0))
				.executeUpdate();
		em.clear();

		assertEquals(List.of(primeira.getId(), segunda.getId()), idsDaFila());
	}

	// --- Carregamento ---

	@Test
	void clienteVemNaMesmaConsultaDaFila() {
		conversaNaFila("5581000000001");
		em.flush();
		em.clear();

		Conversa daFila = repositorio.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO).get(0);

		assertTrue(Hibernate.isInitialized(daFila.getCliente()));
	}

	@Test
	void conversaSalvaVoltaDoBancoComClienteEMensagensEmOrdem() {
		Conversa conversa = conversaComBot("5581000000001");
		conversa.adicionarMensagem("Oi, preciso da segunda via", OrigemMensagem.CLIENTE);
		conversa.adicionarMensagem("Claro! Qual o CPF?", OrigemMensagem.BOT);
		em.flush();
		em.clear();

		Conversa doBanco = repositorio.findById(conversa.getId()).orElseThrow();

		assertEquals("5581000000001", doBanco.getTelefoneCliente());
		assertEquals(StatusConversa.EM_ATENDIMENTO_BOT, doBanco.getStatus());
		assertEquals(List.of(OrigemMensagem.CLIENTE, OrigemMensagem.BOT),
				doBanco.getMensagens().stream().map(Mensagem::getOrigem).toList());
	}

}
