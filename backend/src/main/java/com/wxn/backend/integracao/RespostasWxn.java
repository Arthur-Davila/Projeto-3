package com.wxn.backend.integracao;

import java.text.Normalizer;
import java.util.Locale;

final class RespostasWxn {

	private RespostasWxn() {
	}

	static RespostaInicial para(String mensagem) {
		String texto = normalizar(mensagem).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();

		if (texto.matches("^(oi|ola|bom dia|boa tarde|boa noite|opa|tudo bem|oi tudo bem)$")) {
			return RespostaInicial.conversa("Olá! Sou o assistente virtual da WXN. Posso ajudar com boleto, fatura, acesso ao aplicativo ou cancelamento. O que você precisa?");
		}
		if (contem(texto, "cancelar", "cancelamento", "encerrar meu plano", "parar meu plano")) {
			return RespostaInicial.transferir(
					"Vou encaminhar seu pedido ao time de Retenção, que poderá confirmar as condições do seu plano. Não envie senha ou dados de cartão por aqui.",
					"Cancelamento de plano", "Retenção");
		}
		if (contem(texto, "boleto", "segunda via", "2 via", "linha digitavel")) {
			return RespostaInicial.transferir(
					"Vou encaminhar você ao Financeiro para ajudar com a segunda via do boleto. Por segurança, não envie senha, código de verificação ou número completo do cartão.",
					"Segunda via de boleto", "Financeiro");
		}
		if (contem(texto, "fatura", "cobranca", "pagamento", "valor diferente", "cobrado")) {
			return RespostaInicial.transferir(
					"Vou encaminhar sua dúvida ao Financeiro para conferirem a fatura. Se quiser, informe qual lançamento está diferente, sem enviar dados de cartão.",
					"Dúvida sobre fatura", "Financeiro");
		}
		if (contem(texto, "acesso", "login", "senha", "entrar no app", "aplicativo", "nao consigo entrar")) {
			if (pediuAtendente(texto)) {
				return RespostaInicial.transferir(
						"Claro, vou encaminhar o problema de acesso ao Suporte. Não compartilhe sua senha nem códigos de verificação.",
						"Problema de acesso ao aplicativo", "Suporte");
			}
			return RespostaInicial.conversa(
					"Entendi que você está com dificuldade de acesso. Não compartilhe sua senha nem códigos de verificação; qual mensagem de erro aparece no aplicativo?");
		}
		if (pediuAtendente(texto)) {
			return RespostaInicial.transferir(
					"Claro, vou encaminhar sua conversa para um atendente. Assim que alguém estiver disponível, continua por aqui.",
					"Solicitação de atendimento humano", "Suporte");
		}
		return null;
	}

	static String respostaDeReserva() {
		return "Posso ajudar com boleto, fatura, acesso ao aplicativo ou cancelamento de plano. Qual desses assuntos você quer tratar? Se preferir falar com uma pessoa, escreva “atendente”.";
	}

	private static boolean contem(String texto, String... termos) {
		for (String termo : termos) {
			if (texto.contains(termo)) return true;
		}
		return false;
	}

	private static boolean pediuAtendente(String texto) {
		return contem(texto, "atendente", "humano", "pessoa", "falar com alguem", "falar com uma pessoa", "suporte humano");
	}

	private static String normalizar(String valor) {
		return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toLowerCase(Locale.ROOT);
	}
}
