package com.wxn.backend.fila.servico;

public class ConversaNaoEncontradaException extends RuntimeException {

	public ConversaNaoEncontradaException(Long id) {
		super("Conversa não encontrada: " + id);
	}

}
