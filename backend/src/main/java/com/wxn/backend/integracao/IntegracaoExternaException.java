package com.wxn.backend.integracao;

public class IntegracaoExternaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IntegracaoExternaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
