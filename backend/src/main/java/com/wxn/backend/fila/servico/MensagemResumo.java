package com.wxn.backend.fila.servico;

import java.time.LocalDateTime;

import com.wxn.backend.fila.dominio.Mensagem;
import com.wxn.backend.fila.dominio.OrigemMensagem;

// Histórico exibido no painel; a entidade Mensagem não sai do serviço.
public record MensagemResumo(
		Long id,
		String conteudo,
		OrigemMensagem origem,
		LocalDateTime enviadaEm) {

	static MensagemResumo de(Mensagem mensagem) {
		return new MensagemResumo(
				mensagem.getId(),
				mensagem.getConteudo(),
				mensagem.getOrigem(),
				mensagem.getEnviadaEm());
	}
}
