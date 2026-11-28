package com.wxn.backend.integracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import com.wxn.backend.fila.dominio.Mensagem;
import com.wxn.backend.fila.dominio.OrigemMensagem;

@Component
public class GroqApiClient {

	private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);
	private static final String INSTRUCOES = """
			Você é o assistente virtual de atendimento da WXN. Responda em português do Brasil,
			com clareza, gentileza e no máximo três frases. Use apenas informações que o cliente
			forneceu e os fatos conhecidos: a WXN atende dúvidas sobre segunda via de boleto,
			acesso ao aplicativo, fatura e cancelamento de plano; as equipes são Financeiro,
			Suporte e Retenção. Não invente preços, prazos, horários, regras de multa, dados de
			conta ou ações que não foram realizadas. Quando faltar informação específica, diga
			isso claramente e oriente o cliente a escrever “atendente” se quiser falar com uma pessoa. Nunca peça senha, código de
			verificação, número completo de cartão ou outros dados de autenticação. Não afirme
			que encaminhou a conversa; o sistema só faz isso quando informa explicitamente.
			""";

	private final RestClient restClient;
	private final String apiKey;
	private final String model;

	public GroqApiClient(
			RestClient.Builder restClientBuilder,
			@Value("${wxn.groq.api-key:}") String apiKey,
			@Value("${wxn.groq.model:llama-3.3-70b-versatile}") String model) {
		this.restClient = restClientBuilder.build();
		this.apiKey = apiKey.trim();
		this.model = model.trim();
	}

	public boolean isConfigured() {
		return !apiKey.isBlank();
	}

	public String responder(List<Mensagem> historico, String faqWxn) {
		if (!isConfigured()) {
			throw new IntegracaoExternaException("A chave da API do Groq não está configurada.", null);
		}

		List<Map<String, String>> messages = new ArrayList<>();
		String contextoFaq = faqWxn == null || faqWxn.isBlank() ? "" : "\n\nInformações aprovadas pela WXN:\n" + faqWxn;
		messages.add(Map.of("role", "system", "content", INSTRUCOES + contextoFaq));
		for (Mensagem mensagem : historico.stream().skip(Math.max(0, historico.size() - 16L)).toList()) {
			String role = mensagem.getOrigem() == OrigemMensagem.CLIENTE ? "user" : "assistant";
			messages.add(Map.of("role", role, "content", mensagem.getConteudo()));
		}

		Map<String, Object> payload = Map.of(
				"model", model,
				"messages", messages,
				"max_tokens", 300);

		try {
			JsonNode resposta = restClient.post()
					.uri("https://api.groq.com/openai/v1/chat/completions")
					.header("Authorization", "Bearer " + apiKey)
					.body(payload)
					.retrieve()
					.body(JsonNode.class);
			String texto = extrairTexto(resposta);
			if (texto.isBlank()) {
				throw new IntegracaoExternaException("O Groq retornou uma resposta vazia.", null);
			}
			return texto;
		} catch (IntegracaoExternaException erro) {
			throw erro;
		} catch (RuntimeException erro) {
			log.warn("Falha ao obter resposta da API do Groq.");
			throw new IntegracaoExternaException("Não foi possível obter uma resposta do Groq.", erro);
		}
	}

	private String extrairTexto(JsonNode resposta) {
		if (resposta == null) return "";
		JsonNode content = resposta.path("choices").path(0).path("message").path("content");
		return content.isTextual() ? content.asText().trim() : "";
	}
}
