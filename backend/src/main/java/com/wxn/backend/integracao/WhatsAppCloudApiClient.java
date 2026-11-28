package com.wxn.backend.integracao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;

@Component
public class WhatsAppCloudApiClient {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudApiClient.class);

	private final RestClient restClient;
	private final String accessToken;
	private final String phoneNumberId;
	private final String graphVersion;

	public WhatsAppCloudApiClient(
			RestClient.Builder restClientBuilder,
			@Value("${wxn.whatsapp.access-token:}") String accessToken,
			@Value("${wxn.whatsapp.phone-number-id:}") String phoneNumberId,
			@Value("${wxn.whatsapp.graph-version:v27.0}") String graphVersion) {
		this.restClient = restClientBuilder.build();
		this.accessToken = accessToken.trim();
		this.phoneNumberId = phoneNumberId.trim();
		this.graphVersion = graphVersion.trim();
	}

	public boolean isConfigured() {
		return !accessToken.isBlank() && !phoneNumberId.isBlank();
	}

	public boolean acceptsPhoneNumberId(String incomingPhoneNumberId) {
		return phoneNumberId.isBlank() || phoneNumberId.equals(incomingPhoneNumberId);
	}

	public Optional<String> sendText(String recipient, String text) {
		if (!isConfigured()) {
			log.warn("WhatsApp Cloud API não configurada; mensagem mantida apenas no histórico local.");
			return Optional.empty();
		}
		if (recipient == null || recipient.isBlank() || text == null || text.isBlank()) {
			throw new IllegalArgumentException("Destinatário e conteúdo são obrigatórios para enviar WhatsApp.");
		}

		String identificador = recipient.trim();
		Map<String, Object> payload = new HashMap<>();
		payload.put("messaging_product", "whatsapp");
		payload.put("recipient_type", "individual");
		if (identificador.matches("US\\.(?:ENT\\.)?[A-Za-z0-9]+")) {
			payload.put("recipient", identificador);
		} else {
			if (!identificador.startsWith("+") && identificador.matches("\\d+")) {
				identificador = "+" + identificador;
			}
			payload.put("to", identificador);
		}
		payload.put("type", "text");
		payload.put("text", Map.of("preview_url", false, "body", text));

		try {
			JsonNode resposta = restClient.post()
					.uri("https://graph.facebook.com/{version}/{phoneNumberId}/messages", graphVersion, phoneNumberId)
					.header("Authorization", "Bearer " + accessToken)
					.body(payload)
					.retrieve()
					.body(JsonNode.class);
			JsonNode id = resposta == null ? null : resposta.path("messages").path(0).path("id");
			return id != null && id.isTextual() ? Optional.of(id.asText()) : Optional.empty();
		} catch (RestClientResponseException erro) {
			log.warn("WhatsApp Cloud API recusou o envio (HTTP {}).", erro.getStatusCode().value());
			throw new IntegracaoExternaException(
					"O WhatsApp não aceitou o envio. Confira o token, o número e a janela de atendimento.", erro);
		} catch (RuntimeException erro) {
			if (erro instanceof IntegracaoExternaException integracao) {
				throw integracao;
			}
			log.warn("Falha de comunicação com a WhatsApp Cloud API.");
			throw new IntegracaoExternaException("Não foi possível enviar a mensagem pelo WhatsApp.", erro);
		}
	}
}
