package com.wxn.backend.fila.servico;

// Unchecked, como a TransicaoInvalidaException: a camada web traduz para 404.
public class ConversaNaoEncontradaException extends RuntimeException {

	public ConversaNaoEncontradaException(Long id) {
		super("Conversa não encontrada: " + id);
	}

}
