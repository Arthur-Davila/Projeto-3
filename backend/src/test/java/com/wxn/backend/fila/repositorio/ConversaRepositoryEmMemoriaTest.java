package com.wxn.backend.fila.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

class ConversaRepositoryEmMemoriaTest {

	private ConversaRepositoryEmMemoria repositorio;

	@BeforeEach
	void setUp() {
		repositorio = new ConversaRepositoryEmMemoria();
	}

	// Garante horários de entrada distintos sem depender da resolução do relógio do SO.
	private void esperarRelogioAvancar(LocalDateTime referencia) {
		while (!LocalDateTime.now().isAfter(referencia)) {
			Thread.onSpinWait();
		}
	}

	// --- salvar ---

	@Test
	void salvarAtribuiIdANovaConversa() {
		Conversa conversa = repositorio.salvar(new Conversa("5581111110000"));

		assertNotNull(conversa.getId());
	}

	@Test
	void conversasDiferentesRecebemIdsDiferentes() {
		Conversa primeira = repositorio.salvar(new Conversa("5581111110000"));
		Conversa segunda = repositorio.salvar(new Conversa("5581222220000"));

		assertNotEquals(primeira.getId(), segunda.getId());
	}

	@Test
	void salvarDeNovoMantemIdENaoDuplica() {
		Conversa conversa = repositorio.salvar(new Conversa("5581111110000"));
		Long id = conversa.getId();
		conversa.encaminharParaHumano();

		repositorio.salvar(conversa);

		assertEquals(id, conversa.getId());
		assertEquals(1, repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.AGUARDANDO_HUMANO).size());
		assertTrue(repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.EM_ATENDIMENTO_BOT).isEmpty());
	}

	@Test
	void naoPermiteSalvarConversaNula() {
		assertThrows(IllegalArgumentException.class, () -> repositorio.salvar(null));
	}

	// --- buscarPorId ---

	@Test
	void buscarPorIdEncontraConversaSalva() {
		Conversa conversa = repositorio.salvar(new Conversa("5581111110000"));

		assertSame(conversa, repositorio.buscarPorId(conversa.getId()).orElseThrow());
	}

	@Test
	void buscarPorIdInexistenteDevolveVazio() {
		assertTrue(repositorio.buscarPorId(999L).isEmpty());
	}

	@Test
	void buscarPorIdNuloDevolveVazio() {
		assertTrue(repositorio.buscarPorId(null).isEmpty());
	}

	// --- buscarPorStatusOrdenadoPorEntrada ---

	@Test
	void filtraPorStatus() {
		Conversa comBot = repositorio.salvar(new Conversa("5581111110000"));
		Conversa naFila = new Conversa("5581222220000");
		naFila.encaminharParaHumano();
		repositorio.salvar(naFila);

		assertEquals(List.of(naFila), repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.AGUARDANDO_HUMANO));
		assertEquals(List.of(comBot), repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.EM_ATENDIMENTO_BOT));
	}

	@Test
	void ordenaPelaEntradaNaFilaENaoPeloId() {
		// "primeiraSalva" recebe o menor id, mas entra na fila por último.
		Conversa primeiraSalva = repositorio.salvar(new Conversa("5581111110000"));
		Conversa segundaSalva = repositorio.salvar(new Conversa("5581222220000"));

		segundaSalva.encaminharParaHumano();
		esperarRelogioAvancar(segundaSalva.getEntrouNaFilaEm());
		primeiraSalva.encaminharParaHumano();

		assertEquals(List.of(segundaSalva, primeiraSalva),
				repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.AGUARDANDO_HUMANO));
	}

	@Test
	void listaDevolvidaNaoPodeSerModificada() {
		repositorio.salvar(new Conversa("5581111110000"));
		List<Conversa> lista = repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.EM_ATENDIMENTO_BOT);

		assertThrows(UnsupportedOperationException.class, lista::clear);
	}

}
