package com.wxn.backend.fila.servico;

// Separada da TransicaoInvalidaException porque o painel mostra o aviso "já assumida por outra pessoa".
public class ConversaJaAssumidaException extends RuntimeException {

	public ConversaJaAssumidaException(Long conversaId) {
		super("Conversa " + conversaId + " já foi assumida por outro atendente");
	}

}
