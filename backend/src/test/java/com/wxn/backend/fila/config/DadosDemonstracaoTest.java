package com.wxn.backend.fila.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wxn.backend.fila.repositorio.AtendenteRepository;
import com.wxn.backend.fila.servico.FilaConversasService;

// Banco próprio para o cenário de demonstração não interferir nos outros testes.
@SpringBootTest(properties = {
		"wxn.dados-demo=true",
		"spring.datasource.url=jdbc:h2:mem:demo_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
class DadosDemonstracaoTest {

	@Autowired
	private AtendenteRepository atendentes;

	@Autowired
	private FilaConversasService servico;

	@Test
	void criaFilaEAtendimentosDeExemplo() {
		assertEquals(3, atendentes.count());
		assertEquals(3, servico.listarFila().size());
		assertTrue(servico.listarFila().stream().allMatch(item -> item.tempoDeEspera().toSeconds() >= 60));

		Long anaId = atendentes.findAllByOrderByNomeAsc().stream()
				.filter(atendente -> atendente.getNome().equals("Ana Lima"))
				.findFirst()
				.orElseThrow()
				.getId();

		assertEquals(1, servico.listarAtendimentosDoAtendente(anaId).size());
		assertFalse(servico.detalhar(servico.listarAtendimentosDoAtendente(anaId).get(0).conversaId())
				.mensagens().isEmpty());
	}
}
