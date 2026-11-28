package com.wxn.backend.integracao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

	private final WhatsAppWebhookService webhook;
	private final ObjectMapper objectMapper;
	private final String verifyToken;
	private final String appSecret;

	public WhatsAppWebhookController(
			WhatsAppWebhookService webhook,
			ObjectMapper objectMapper,
			@Value("${wxn.whatsapp.verify-token:}") String verifyToken,
			@Value("${wxn.whatsapp.app-secret:}") String appSecret) {
		this.webhook = webhook;
		this.objectMapper = objectMapper;
		this.verifyToken = verifyToken.trim();
		this.appSecret = appSecret.trim();
	}

	@GetMapping
	public ResponseEntity<String> verificar(
			@RequestParam(name = "hub.mode", required = false) String modo,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String desafio) {
		if (verifyToken.isBlank()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Configure WHATSAPP_VERIFY_TOKEN.");
		}
		if (!"subscribe".equals(modo) || !verifyToken.equals(token) || desafio == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verificação do webhook recusada.");
		}
		return ResponseEntity.ok(desafio);
	}

	@PostMapping
	public ResponseEntity<String> receber(
			@RequestBody byte[] corpo,
			@RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
		if (appSecret.isBlank()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Configure WHATSAPP_APP_SECRET.");
		}
		if (!assinaturaValida(corpo, assinatura)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Assinatura do webhook inválida.");
		}
		try {
			JsonNode payload = objectMapper.readTree(corpo);
			webhook.processar(payload);
			return ResponseEntity.ok("EVENT_RECEIVED");
		} catch (JacksonException erro) {
			return ResponseEntity.badRequest().body("Payload JSON inválido.");
		}
	}

	private boolean assinaturaValida(byte[] corpo, String assinatura) {
		if (assinatura == null || !assinatura.startsWith("sha256=")) return false;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			String esperada = "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo));
			return MessageDigest.isEqual(
					esperada.getBytes(StandardCharsets.US_ASCII), assinatura.getBytes(StandardCharsets.US_ASCII));
		} catch (Exception erro) {
			return false;
		}
	}
}
