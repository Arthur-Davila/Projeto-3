package com.wxn.backend.fila.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.OrigemMensagem;
import com.wxn.backend.fila.dominio.StatusConversa;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FilaControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EntityManager em;

	private Long conversaId;
	private Long atendenteId;

	@BeforeEach
	void prepararCenario() {
		Atendente ana = new Atendente("Ana Lima", "ana@wxn.com", "Suporte");
		em.persist(ana);
		Cliente maria = new Cliente("Maria Silva", "5581999990000");
		em.persist(maria);

		Conversa conversa = new Conversa(maria);
		conversa.definirTriagem("Segunda via de boleto", "Financeiro");
		conversa.adicionarMensagem("Oi, preciso da segunda via", OrigemMensagem.CLIENTE);
		conversa.adicionarMensagem("Claro, vou encaminhar", OrigemMensagem.BOT);
		conversa.encaminharParaHumano();
		em.persist(conversa);
		em.flush();

		conversaId = conversa.getId();
		atendenteId = ana.getId();
	}

	@Test
	void listaFilaComOsDadosQueOPainelMostra() throws Exception {
		mvc.perform(get("/api/fila"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].conversaId").value(conversaId))
				.andExpect(jsonPath("$[0].nomeCliente").value("Maria Silva"))
				.andExpect(jsonPath("$[0].telefoneCliente").value("5581999990000"))
				.andExpect(jsonPath("$[0].assunto").value("Segunda via de boleto"))
				.andExpect(jsonPath("$[0].equipe").value("Financeiro"))
				.andExpect(jsonPath("$[0].esperaEmSegundos").isNumber());
	}

	@Test
	void listaAtendentesParaOSeletorDoPainel() throws Exception {
		mvc.perform(get("/api/atendentes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(atendenteId))
				.andExpect(jsonPath("$[0].nome").value("Ana Lima"))
				.andExpect(jsonPath("$[0].equipe").value("Suporte"))
				.andExpect(jsonPath("$[0].disponivel").value(true));
	}

	@Test
	void assumeConversaEMostraEmMeusAtendimentos() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(StatusConversa.EM_ATENDIMENTO_HUMANO.name()))
				.andExpect(jsonPath("$.atendenteId").value(atendenteId))
				.andExpect(jsonPath("$.nomeAtendente").value("Ana Lima"));

		mvc.perform(get("/api/atendimentos").param("atendenteId", atendenteId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].conversaId").value(conversaId))
				.andExpect(jsonPath("$[0].status").value(StatusConversa.EM_ATENDIMENTO_HUMANO.name()));
	}

	@Test
	void segundaAssuncaoDevolveConflito() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + "}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/fila/{id}/assumir", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + "}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem", containsString("já foi assumida")));
	}

	@Test
	void assuntoInexistenteDevolveNaoEncontrado() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", 999_999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + "}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem", containsString("não encontrada")));
	}

	@Test
	void assumirSemAtendenteDevolveRequisicaoInvalida() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Atendente é obrigatório"));
	}

	@Test
	void detalheTrazTodoOHistoricoDaConversa() throws Exception {
		mvc.perform(get("/api/conversas/{id}", conversaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversaId").value(conversaId))
				.andExpect(jsonPath("$.mensagens.length()").value(2))
				.andExpect(jsonPath("$.mensagens[0].conteudo").value("Oi, preciso da segunda via"))
				.andExpect(jsonPath("$.mensagens[0].origem").value("CLIENTE"))
				.andExpect(jsonPath("$.mensagens[1].origem").value("BOT"));
	}

	@Test
	void atendenteAssumeEEnviaRespostaQueFicaNoHistorico() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + "}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/conversas/{id}/mensagens", conversaId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\": " + atendenteId + ", \"conteudo\": \"Vou te ajudar agora.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagens.length()").value(3))
				.andExpect(jsonPath("$.mensagens[2].conteudo").value("Vou te ajudar agora."))
				.andExpect(jsonPath("$.mensagens[2].origem").value("ATENDENTE"));
	}
}
