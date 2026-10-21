package com.wxn.backend.fila.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;
import com.wxn.backend.fila.dominio.TransicaoInvalidaException;
import com.wxn.backend.fila.repositorio.ConversaRepository;
import com.wxn.backend.fila.repositorio.ConversaRepositoryEmMemoria;

// Sem Spring nem banco: o serviço recebe a versão em memória pelo construtor.
class FilaConversasServiceTest {

	private ConversaRepository repositorio;
	private FilaConversasService servico;

	@BeforeEach
	void setUp() {
		repositorio = new ConversaRepositoryEmMemoria();
		servico = new FilaConversasService(repositorio);
	}

	private Conversa salvarNaFila(String telefone) {
		Conversa conversa = new Conversa(telefone);
		conversa.encaminharParaHumano();
		return repositorio.salvar(conversa);
	}

	// Garante horários de entrada distintos sem depender da resolução do relógio do SO.
	private void esperarRelogioAvancar(LocalDateTime referencia) {
		while (!LocalDateTime.now().isAfter(referencia)) {
			Thread.onSpinWait();
		}
	}

	// --- listarFila ---

	@Test
	void filaVaziaQuandoNinguemAguardaHumano() {
		repositorio.salvar(new Conversa("5581111110000"));

		assertTrue(servico.listarFila().isEmpty());
	}

	@Test
	void filaTemSoQuemAguardaHumanoNaOrdemDeEntrada() {
		Conversa primeira = salvarNaFila("5581111110000");
		esperarRelogioAvancar(primeira.getEntrouNaFilaEm());
		Conversa segunda = salvarNaFila("5581222220000");
		repositorio.salvar(new Conversa("5581333330000"));

		assertEquals(List.of(primeira, segunda), servico.listarFila());
	}

	@Test
	void conversaAssumidaSaiDaFilaSozinha() {
		Conversa primeira = salvarNaFila("5581111110000");
		Conversa segunda = salvarNaFila("5581222220000");

		servico.assumir(primeira.getId(), 7L);

		assertEquals(List.of(segunda), servico.listarFila());
	}

	// --- buscarPorId ---

	@Test
	void buscarPorIdDevolveConversa() {
		Conversa conversa = salvarNaFila("5581111110000");

		assertSame(conversa, servico.buscarPorId(conversa.getId()));
	}

	@Test
	void buscarPorIdInexistenteLancaExcecao() {
		assertThrows(ConversaNaoEncontradaException.class, () -> servico.buscarPorId(999L));
	}

	// --- assumir ---

	@Test
	void assumirPassaConversaParaOAtendente() {
		Conversa conversa = salvarNaFila("5581111110000");

		Conversa assumida = servico.assumir(conversa.getId(), 7L);

		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, assumida.getStatus());
		assertEquals(7L, assumida.getAtendenteId());
		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO,
				repositorio.buscarPorId(conversa.getId()).orElseThrow().getStatus());
	}

	@Test
	void assumirConversaInexistenteLancaExcecao() {
		assertThrows(ConversaNaoEncontradaException.class, () -> servico.assumir(999L, 7L));
	}

	@Test
	void segundoAtendenteNaoConsegueAssumirConversaJaAssumida() {
		Conversa conversa = salvarNaFila("5581111110000");
		servico.assumir(conversa.getId(), 7L);

		assertThrows(TransicaoInvalidaException.class, () -> servico.assumir(conversa.getId(), 8L));
		assertEquals(7L, servico.buscarPorId(conversa.getId()).getAtendenteId());
	}

	@Test
	void naoAssumeConversaQueAindaEstaComOBot() {
		Conversa conversa = repositorio.salvar(new Conversa("5581111110000"));

		assertThrows(TransicaoInvalidaException.class, () -> servico.assumir(conversa.getId(), 7L));
	}

}
