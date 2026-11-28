package com.wxn.backend.integracao;

record RespostaInicial(String texto, String assunto, String equipe, boolean encaminhar) {

	static RespostaInicial conversa(String texto) {
		return new RespostaInicial(texto, null, null, false);
	}

	static RespostaInicial transferir(String texto, String assunto, String equipe) {
		return new RespostaInicial(texto, assunto, equipe, true);
	}
}
