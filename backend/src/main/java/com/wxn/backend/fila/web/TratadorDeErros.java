package com.wxn.backend.fila.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wxn.backend.fila.dominio.TransicaoInvalidaException;
import com.wxn.backend.fila.servico.AtendenteNaoEncontradoException;
import com.wxn.backend.fila.servico.ConversaJaAssumidaException;
import com.wxn.backend.fila.servico.ConversaNaoEncontradaException;

// Traduz as exceções de negócio para respostas que o painel sabe exibir.
@RestControllerAdvice(assignableTypes = FilaController.class)
public class TratadorDeErros {

	@ExceptionHandler({ ConversaNaoEncontradaException.class, AtendenteNaoEncontradoException.class })
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErroResposta naoEncontrado(RuntimeException excecao) {
		return new ErroResposta(excecao.getMessage());
	}

	@ExceptionHandler({ ConversaJaAssumidaException.class, TransicaoInvalidaException.class })
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErroResposta conflito(RuntimeException excecao) {
		return new ErroResposta(excecao.getMessage());
	}

	@ExceptionHandler({
			IllegalArgumentException.class,
			HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErroResposta requisicaoInvalida(Exception excecao) {
		if (excecao instanceof IllegalArgumentException) {
			return new ErroResposta(excecao.getMessage());
		}
		return new ErroResposta("Requisição inválida");
	}
}
