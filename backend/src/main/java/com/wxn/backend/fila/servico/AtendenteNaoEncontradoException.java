package com.wxn.backend.fila.servico;

public class AtendenteNaoEncontradoException extends RuntimeException {

	public AtendenteNaoEncontradoException(Long id) {
		super("Atendente não encontrado: " + id);
	}

}
