package com.wxn.backend.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class WhatsAppWebhookService {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);
	private final WhatsAppCloudApiClient whatsapp;
	private final ProcessadorMensagemWhatsApp processador;

	public WhatsAppWebhookService(WhatsAppCloudApiClient whatsapp, ProcessadorMensagemWhatsApp processador) {
		this.whatsapp = whatsapp;
		this.processador = processador;
	}

	public void processar(JsonNode payload) {
		for (JsonNode entrada : payload.path("entry")) {
			for (JsonNode alteracao : entrada.path("changes")) {
				JsonNode valor = alteracao.path("value");
				if (!"messages".equals(alteracao.path("field").asText())) continue;
				String phoneNumberId = valor.path("metadata").path("phone_number_id").asText("");
				if (!whatsapp.acceptsPhoneNumberId(phoneNumberId)) {
					log.warn("Webhook recebido para um número que não está configurado; evento ignorado.");
					continue;
				}
				for (JsonNode mensagem : valor.path("messages")) {
					if (!"text".equals(mensagem.path("type").asText())) {
						log.info("Mensagem de WhatsApp não textual ignorada.");
						continue;
					}
					String telefone = mensagem.path("from").asText("");
					if (telefone.isBlank()) telefone = mensagem.path("from_user_id").asText("");
					String id = mensagem.path("id").asText("");
					String texto = mensagem.path("text").path("body").asText("");
					String nome = encontrarNome(valor.path("contacts"), telefone);
					processador.processar(telefone, nome, id, texto);
				}
			}
		}
	}

	private String encontrarNome(JsonNode contatos, String telefone) {
		for (JsonNode contato : contatos) {
			String waId = contato.path("wa_id").asText("");
			String fromUserId = contato.path("user_id").asText("");
			if (telefone.equals(waId) || telefone.equals(fromUserId)) {
				return contato.path("profile").path("name").asText("Cliente WhatsApp");
			}
		}
		return "Cliente WhatsApp";
	}
}
