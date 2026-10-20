package com.wxn.backend.fila.dominio;

// Unchecked: transição inválida é violação de regra de negócio, não algo que
// todo chamador deva tratar. A camada web traduz para uma resposta HTTP.
public class TransicaoInvalidaException extends RuntimeException {

	public TransicaoInvalidaException(String mensagem) {
		super(mensagem);
	}

}
