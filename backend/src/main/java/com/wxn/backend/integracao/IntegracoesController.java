package com.wxn.backend.integracao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/integracoes")
public class IntegracoesController {

	private final WhatsAppCloudApiClient whatsapp;
	private final GroqApiClient groq;
	private final boolean webhookConfigurado;

	public IntegracoesController(
			WhatsAppCloudApiClient whatsapp,
			GroqApiClient groq,
			@Value("${wxn.whatsapp.verify-token:}") String verifyToken,
			@Value("${wxn.whatsapp.app-secret:}") String appSecret) {
		this.whatsapp = whatsapp;
		this.groq = groq;
		this.webhookConfigurado = !verifyToken.isBlank() && !appSecret.isBlank();
	}

	@GetMapping
	public StatusIntegracoes status() {
		return new StatusIntegracoes(whatsapp.isConfigured() && webhookConfigurado, groq.isConfigured());
	}

	public record StatusIntegracoes(boolean whatsappConfigurado, boolean groqConfigurado) {
	}
}
