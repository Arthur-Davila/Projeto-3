package com.wxn.backend.fila.config;

import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.OrigemMensagem;

import jakarta.persistence.EntityManager;

/**
 * Popula o painel na primeira execução para que a tela da segunda história
 * possa ser avaliada sem cadastro manual. Só roda com o banco vazio e pode
 * ser desligado com WXN_DADOS_DEMO=false.
 */
@Component
@ConditionalOnProperty(name = "wxn.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosDemonstracao implements ApplicationRunner {

	private final EntityManager em;
	private final TransactionTemplate transacao;

	public DadosDemonstracao(EntityManager em, TransactionTemplate transacao) {
		this.em = em;
		this.transacao = transacao;
	}

	@Override
	public void run(ApplicationArguments args) {
		transacao.executeWithoutResult(status -> {
			if (em.createQuery("select count(a) from Atendente a", Long.class).getSingleResult() > 0) {
				return;
			}
			criarDados();
		});
	}

	private void criarDados() {
		Atendente ana = persistir(new Atendente("Ana Lima", "ana.lima@wxn.com", "Suporte"));
		persistir(new Atendente("Bruno Reis", "bruno.reis@wxn.com", "Financeiro"));
		Atendente carla = persistir(new Atendente("Carla Mendes", "carla.mendes@wxn.com", "Retenção"));
		carla.ficarIndisponivel();

		Conversa boleto = conversaNaFila(
				persistir(new Cliente("Maria Silva", "5581999990000")),
				"Segunda via de boleto", "Financeiro",
				"Oi! Preciso da segunda via do boleto de outubro.",
				"Claro. Vou encaminhar você para o time Financeiro.");
		Conversa acesso = conversaNaFila(
				persistir(new Cliente("João Pereira", "5581988887777")),
				"Problema com acesso ao aplicativo", "Suporte",
				"Não consigo entrar na minha conta pelo aplicativo.",
				"Entendi. Vou chamar um atendente para ajudar com o acesso.");
		Conversa cancelamento = conversaNaFila(
				persistir(new Cliente("Lúcia Alves", "5581977776666")),
				"Cancelamento de plano", "Retenção",
				"Quero cancelar meu plano e saber se tem multa.");
		Conversa fatura = conversaNaFila(
				persistir(new Cliente("Pedro Santos", "5581966665555")),
				"Dúvida sobre a fatura", "Financeiro",
				"A fatura veio com um valor diferente do combinado.",
				"Vou transferir para o Financeiro conferir com você.");

		em.flush();

		// A conversa do Pedro já estava com a Ana quando o painel foi aberto.
		fatura.assumirPor(ana);
		em.flush();

		// O domínio não expõe alteração de horário; no dado de demonstração
		// recuamos a entrada na fila para o painel mostrar esperas diferentes.
		definirEspera(boleto, 14);
		definirEspera(acesso, 8);
		definirEspera(cancelamento, 3);
		definirEspera(fatura, 22);
		em.clear();
	}

	private Conversa conversaNaFila(Cliente cliente, String assunto, String equipe, String... mensagens) {
		Conversa conversa = new Conversa(cliente);
		conversa.definirTriagem(assunto, equipe);
		if (mensagens.length > 0) {
			conversa.adicionarMensagem(mensagens[0], OrigemMensagem.CLIENTE);
		}
		if (mensagens.length > 1) {
			conversa.adicionarMensagem(mensagens[1], OrigemMensagem.BOT);
		}
		conversa.encaminharParaHumano();
		return persistir(conversa);
	}

	private <T> T persistir(T entidade) {
		em.persist(entidade);
		return entidade;
	}

	private void definirEspera(Conversa conversa, long minutos) {
		em.createQuery("update Conversa c set c.entrouNaFilaEm = :momento where c.id = :id")
				.setParameter("momento", LocalDateTime.now().minusMinutes(minutos))
				.setParameter("id", conversa.getId())
				.executeUpdate();
	}
}
