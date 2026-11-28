package com.wxn.backend.integracao;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.backend.fila.dominio.Chatbot;
import com.wxn.backend.fila.dominio.Cliente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.Mensagem;
import com.wxn.backend.fila.dominio.OrigemMensagem;
import com.wxn.backend.fila.dominio.StatusConversa;
import com.wxn.backend.fila.repositorio.ChatbotRepository;
import com.wxn.backend.fila.repositorio.ClienteRepository;
import com.wxn.backend.fila.repositorio.ConversaRepository;
import com.wxn.backend.fila.repositorio.MensagemRepository;

@Service
public class ProcessadorMensagemWhatsApp {

	private static final Logger log = LoggerFactory.getLogger(ProcessadorMensagemWhatsApp.class);
	private static final List<StatusConversa> CONVERSAS_ABERTAS = List.of(
			StatusConversa.EM_ATENDIMENTO_BOT,
			StatusConversa.AGUARDANDO_HUMANO,
			StatusConversa.EM_ATENDIMENTO_HUMANO);

	private final ClienteRepository clientes;
	private final ChatbotRepository chatbots;
	private final ConversaRepository conversas;
	private final MensagemRepository mensagens;
	private final GroqApiClient groq;
	private final WhatsAppCloudApiClient whatsapp;
	private final String faqWxn;

	public ProcessadorMensagemWhatsApp(
			ClienteRepository clientes,
			ChatbotRepository chatbots,
			ConversaRepository conversas,
			MensagemRepository mensagens,
			GroqApiClient groq,
			WhatsAppCloudApiClient whatsapp,
			@Value("${wxn.chatbot.faq:}") String faqWxn) {
		this.clientes = clientes;
		this.chatbots = chatbots;
		this.conversas = conversas;
		this.mensagens = mensagens;
		this.groq = groq;
		this.whatsapp = whatsapp;
		this.faqWxn = faqWxn;
	}

	@Transactional
	public void processar(String telefoneRecebido, String nomeRecebido, String idWhatsapp, String textoRecebido) {
		if (idWhatsapp == null || idWhatsapp.isBlank() || mensagens.existsByWhatsappMessageId(idWhatsapp)) {
			return;
		}
		String telefone = normalizarIdentificador(telefoneRecebido);
		String texto = textoRecebido == null ? "" : textoRecebido.trim();
		if (telefone.isBlank() || texto.isBlank()) return;

		Cliente cliente = clientes.findByTelefone(telefone).orElseGet(() -> {
			String nome = nomeRecebido == null || nomeRecebido.isBlank() ? "Cliente WhatsApp" : nomeRecebido.trim();
			return clientes.save(new Cliente(nome, telefone));
		});

		Conversa conversa = conversas.findFirstByCliente_TelefoneAndStatusInOrderByIdDesc(telefone, CONVERSAS_ABERTAS)
				.orElseGet(() -> conversas.save(new Conversa(cliente, obterChatbot())));
		conversa.adicionarMensagem(texto, OrigemMensagem.CLIENTE, idWhatsapp);
		conversas.saveAndFlush(conversa);

		if (conversa.getStatus() != StatusConversa.EM_ATENDIMENTO_BOT) {
			return;
		}

		RespostaInicial resposta = RespostasWxn.para(texto);
		String respostaTexto = resposta == null ? gerarResposta(conversa) : resposta.texto();
		if (resposta != null && resposta.encaminhar()) {
			Chatbot chatbot = conversa.getChatbot();
			if (chatbot != null) {
				chatbot.transferirParaFila(conversa, resposta.assunto(), resposta.equipe());
			} else {
				conversa.definirTriagem(resposta.assunto(), resposta.equipe());
				conversa.encaminharParaHumano();
			}
		}

		Optional<String> idResposta = whatsapp.sendText(telefone, respostaTexto);
		conversa.adicionarMensagem(respostaTexto, OrigemMensagem.BOT, idResposta.orElse(null));
		conversas.saveAndFlush(conversa);
	}

	private Chatbot obterChatbot() {
		return chatbots.findFirstByAtivoTrueOrderByIdAsc()
				.orElseGet(() -> chatbots.save(new Chatbot("Assistente virtual WXN")));
	}

	private String normalizarIdentificador(String valor) {
		if (valor == null) return "";
		String identificador = valor.trim();
		if (identificador.matches("US\\.(?:ENT\\.)?[A-Za-z0-9]+")) return identificador;
		return identificador.replaceAll("\\D", "");
	}

	private String gerarResposta(Conversa conversa) {
		if (!groq.isConfigured()) return RespostasWxn.respostaDeReserva();
		try {
			return groq.responder(conversa.getMensagens(), faqWxn);
		} catch (IntegracaoExternaException erro) {
			log.warn("Groq indisponível; será usada a resposta inicial segura.");
			return RespostasWxn.respostaDeReserva();
		}
	}
}
