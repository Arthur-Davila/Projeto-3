package com.wxn.backend.fila.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

// Sem @Transactional no teste: cada atendente precisa da própria transação, como duas requisições HTTP.
@SpringBootTest
class AssumirConcorrenciaTest {

	@Autowired
	private FilaConversasService servico;

	@Autowired
	private EntityManagerFactory emf;

	@Autowired
	private TransactionTemplate transacao;

	@PersistenceContext
	private EntityManager em;

	private record Cenario(Long conversaId, Long atendenteA, Long atendenteB) {
	}

	private Cenario conversaNaFilaComDoisAtendentes() {
		return transacao.execute(status -> {
			Cliente cliente = new Cliente("Maria Silva", "5581999990000");
			em.persist(cliente);
			Conversa conversa = new Conversa(cliente);
			conversa.encaminharParaHumano();
			em.persist(conversa);
			Atendente ana = new Atendente("Ana Lima", "ana@wxn.com", "Suporte");
			Atendente bruno = new Atendente("Bruno Reis", "bruno@wxn.com", "Suporte");
			em.persist(ana);
			em.persist(bruno);
			em.flush();
			return new Cenario(conversa.getId(), ana.getId(), bruno.getId());
		});
	}

	@AfterEach
	void limparBanco() {
		transacao.executeWithoutResult(status -> {
			em.createQuery("delete from Mensagem").executeUpdate();
			em.createQuery("delete from Conversa").executeUpdate();
			em.createQuery("delete from Cliente").executeUpdate();
			em.createQuery("delete from Atendente").executeUpdate();
		});
	}

	@Test
	void doisAtendentesClicandoAoMesmoTempoSoUmVence() throws Exception {
		Cenario cenario = conversaNaFilaComDoisAtendentes();
		CountDownLatch largada = new CountDownLatch(1);
		ExecutorService threads = Executors.newFixedThreadPool(2);
		List<Future<Conversa>> cliques = new ArrayList<>();

		try {
			for (Long atendenteId : List.of(cenario.atendenteA(), cenario.atendenteB())) {
				cliques.add(threads.submit(() -> {
					largada.await();
					return servico.assumir(cenario.conversaId(), atendenteId);
				}));
			}
			largada.countDown();

			int vitorias = 0;
			int recusas = 0;
			for (Future<Conversa> clique : cliques) {
				try {
					clique.get(10, TimeUnit.SECONDS);
					vitorias++;
				} catch (java.util.concurrent.ExecutionException e) {
					assertInstanceOf(ConversaJaAssumidaException.class, e.getCause());
					recusas++;
				}
			}

			assertEquals(1, vitorias);
			assertEquals(1, recusas);
		} finally {
			threads.shutdownNow();
		}

		Conversa gravada = servico.buscarPorId(cenario.conversaId());
		assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, gravada.getStatus());
	}

	@Test
	void bancoRejeitaQuemSalvaComVersaoDesatualizada() {
		// Os dois leem a conversa na fila; A grava primeiro e B tenta gravar a cópia desatualizada.
		Cenario cenario = conversaNaFilaComDoisAtendentes();
		EntityManager atendenteA = emf.createEntityManager();
		EntityManager atendenteB = emf.createEntityManager();

		try {
			atendenteA.getTransaction().begin();
			atendenteB.getTransaction().begin();
			Conversa copiaA = atendenteA.find(Conversa.class, cenario.conversaId());
			Conversa copiaB = atendenteB.find(Conversa.class, cenario.conversaId());

			copiaA.assumirPor(atendenteA.find(Atendente.class, cenario.atendenteA()));
			atendenteA.getTransaction().commit();

			copiaB.assumirPor(atendenteB.find(Atendente.class, cenario.atendenteB()));
			// A regra do domínio passou (a cópia de B ainda está na fila); quem barra é o @Version.
			assertThrows(OptimisticLockException.class, atendenteB::flush);
		} finally {
			if (atendenteB.getTransaction().isActive()) {
				atendenteB.getTransaction().rollback();
			}
			atendenteA.close();
			atendenteB.close();
		}

		assertEquals(cenario.atendenteA(), servico.buscarPorId(cenario.conversaId()).getAtendenteId());
	}

}
