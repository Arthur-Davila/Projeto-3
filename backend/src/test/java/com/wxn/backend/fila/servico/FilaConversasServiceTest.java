package com.wxn.backend.fila.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;
import com.wxn.backend.fila.dominio.TransicaoInvalidaException;
import com.wxn.backend.fila.repositorio.AtendenteRepository;
import com.wxn.backend.fila.repositorio.ConversaRepository;

@ExtendWith(MockitoExtension.class)
class FilaConversasServiceTest {

	private static final Long CONVERSA_ID = 1L;
	private static final Long ATENDENTE_ID = 7L;

	@Mock
	private ConversaRepository conversas;

	@Mock
	private AtendenteRepository atendentes;

	@InjectMocks
	private FilaConversasService servico;

	private Conversa conversaNaFila(String nomeCliente) {
		Conversa conversa = new Conversa(new Cliente(nomeCliente, "5581" + nomeCliente.hashCode()));
		conversa.definirTriagem("Segunda via de boleto", "Financeiro");
		conversa.encaminharParaHumano();
		return conversa;
	}

	private Atendente atendente() {
		return new Atendente("Carlos Souza", "carlos@wxn.com", "Financeiro");
	}

	private void conversaEAtendenteExistem(Conversa conversa, Atendente atendente) {
		when(conversas.findById(CONVERSA_ID)).thenReturn(Optional.of(conversa));
		when(atendentes.findById(ATENDENTE_ID)).thenReturn(Optional.of(atendente));
	}

	// --- listarFila ---

	@Test
	void listarFilaBuscaSoQuemAguardaHumanoEMantemAOrdemDoRepositorio() {
		Conversa maisAntiga = conversaNaFila("Maria");
		Conversa maisNova = conversaNaFila("João");
		when(conversas.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO))
				.thenReturn(List.of(maisAntiga, maisNova));

		List<ItemFila> fila = servico.listarFila();

		assertEquals(List.of("Maria", "João"), fila.stream().map(ItemFila::nomeCliente).toList());
	}

	@Test
	void itemDaFilaTrazOQueOPainelMostra() {
		Conversa conversa = conversaNaFila("Maria");
		when(conversas.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO))
				.thenReturn(List.of(conversa));

		ItemFila item = servico.listarFila().get(0);

		assertEquals("Maria", item.nomeCliente());
		assertEquals(conversa.getTelefoneCliente(), item.telefoneCliente());
		assertEquals("Segunda via de boleto", item.assunto());
		assertEquals("Financeiro", item.equipe());
		assertEquals(conversa.getEntrouNaFilaEm(), item.entrouNaFilaEm());
		assertFalse(item.tempoDeEspera().isNegative());
	}

	@Test
	void filaVaziaDevolveListaVazia() {
		when(conversas.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO))
				.thenReturn(List.of());

		assertTrue(servico.listarFila().isEmpty());
	}

	// --- buscarPorId ---

	@Test
	void buscarPorIdDevolveConversa() {
		Conversa conversa = conversaNaFila("Maria");
		when(conversas.findById(CONVERSA_ID)).thenReturn(Optional.of(conversa));

		assertSame(conversa, servico.buscarPorId(CONVERSA_ID));
	}

	@Test
	void buscarPorIdInexistenteLancaExcecao() {
		when(conversas.findById(CONVERSA_ID)).thenReturn(Optional.empty());

		assertThrows(ConversaNaoEncontradaException.class, () -> servico.buscarPorId(CONVERSA_ID));
	}

	// --- assumir: caminho feliz ---

	@Test
	void assumirPassaConversaParaOAtendenteESalva() {
		Conversa conversa = conversaNaFila("Maria");
		Atendente atendente = atendente();
		conversaEAtendenteExistem(conversa, atendente);
		when(conversas.saveAndFlush(conversa)).thenReturn(conversa);

		Conversa assumida = servico.assumir(CONVERSA_ID, ATENDENTE_ID);

		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, assumida.getStatus());
		assertSame(atendente, assumida.getAtendente());
		verify(conversas).saveAndFlush(conversa);
	}

	// --- assumir: não encontrados ---

	@Test
	void assumirConversaInexistenteLancaExcecao() {
		when(conversas.findById(CONVERSA_ID)).thenReturn(Optional.empty());

		assertThrows(ConversaNaoEncontradaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		verify(conversas, never()).saveAndFlush(any());
	}

	@Test
	void assumirComAtendenteInexistenteLancaExcecaoENaoMexeNaConversa() {
		Conversa conversa = conversaNaFila("Maria");
		when(conversas.findById(CONVERSA_ID)).thenReturn(Optional.of(conversa));
		when(atendentes.findById(ATENDENTE_ID)).thenReturn(Optional.empty());

		assertThrows(AtendenteNaoEncontradoException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		assertEquals(StatusConversa.AGUARDANDO_HUMANO, conversa.getStatus());
		verify(conversas, never()).saveAndFlush(any());
	}

	// --- assumir: já assumida por outra pessoa ---

	@Test
	void conversaJaAssumidaPorOutroAtendenteLancaExcecaoPropria() {
		Conversa conversa = conversaNaFila("Maria");
		Atendente primeiro = new Atendente("Ana Lima", "ana@wxn.com", "Financeiro");
		conversa.assumirPor(primeiro);
		conversaEAtendenteExistem(conversa, atendente());

		assertThrows(ConversaJaAssumidaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		assertSame(primeiro, conversa.getAtendente());
		verify(conversas, never()).saveAndFlush(any());
	}

	@Test
	void conflitoDeVersaoNoBancoViraConversaJaAssumida() {
		Conversa conversa = conversaNaFila("Maria");
		conversaEAtendenteExistem(conversa, atendente());
		when(conversas.saveAndFlush(conversa))
				.thenThrow(new ObjectOptimisticLockingFailureException(Conversa.class, CONVERSA_ID));

		assertThrows(ConversaJaAssumidaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
	}

	// --- assumir: regras que a própria Conversa aplica ---

	@Test
	void naoAssumeConversaQueAindaEstaComOBot() {
		Conversa conversa = new Conversa(new Cliente("Maria", "5581999990000"));
		conversaEAtendenteExistem(conversa, atendente());

		assertThrows(TransicaoInvalidaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		verify(conversas, never()).saveAndFlush(any());
	}

	@Test
	void naoAssumeConversaEncerrada() {
		Conversa conversa = conversaNaFila("Maria");
		conversa.encerrar();
		conversaEAtendenteExistem(conversa, atendente());

		assertThrows(TransicaoInvalidaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		verify(conversas, never()).saveAndFlush(any());
	}

	@Test
	void atendenteIndisponivelNaoAssume() {
		Conversa conversa = conversaNaFila("Maria");
		Atendente atendente = atendente();
		atendente.ficarIndisponivel();
		conversaEAtendenteExistem(conversa, atendente);

		assertThrows(TransicaoInvalidaException.class, () -> servico.assumir(CONVERSA_ID, ATENDENTE_ID));
		assertEquals(StatusConversa.AGUARDANDO_HUMANO, conversa.getStatus());
		verify(conversas, never()).saveAndFlush(any());
	}

}
