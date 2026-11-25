package com.wxn.backend.fila.web;

import java.time.LocalDateTime;

import com.wxn.backend.fila.servico.ItemFila;

// Contrato HTTP da fila. O Duration do serviço é convertido para segundos,
// formato simples de consumir e de recalcular no navegador.
public record ItemFilaResposta(
		Long conversaId,
		String nomeCliente,
		String telefoneCliente,
		String assunto,
		String equipe,
		LocalDateTime entrouNaFilaEm,
		long esperaEmSegundos) {

	static ItemFilaResposta de(ItemFila item) {
		return new ItemFilaResposta(
				item.conversaId(),
				item.nomeCliente(),
				item.telefoneCliente(),
				item.assunto(),
				item.equipe(),
				item.entrouNaFilaEm(),
				Math.max(0, item.tempoDeEspera().toSeconds()));
	}
}
