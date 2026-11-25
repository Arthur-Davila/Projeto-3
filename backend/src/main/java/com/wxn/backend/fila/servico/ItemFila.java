package com.wxn.backend.fila.servico;

import java.time.Duration;
import java.time.LocalDateTime;

import com.wxn.backend.fila.dominio.Conversa;

// O que o painel mostra na coluna "Aguardando"; a entidade JPA não sai do serviço.
public record ItemFila(
		Long conversaId,
		String nomeCliente,
		String telefoneCliente,
		String assunto,
		String equipe,
		LocalDateTime entrouNaFilaEm,
		Duration tempoDeEspera) {

	static ItemFila de(Conversa conversa, LocalDateTime agora) {
		return new ItemFila(
				conversa.getId(),
				conversa.getCliente().getNome(),
				conversa.getCliente().getTelefone(),
				conversa.getAssunto(),
				conversa.getEquipe(),
				conversa.getEntrouNaFilaEm(),
				conversa.tempoDeEspera(agora));
	}

}
