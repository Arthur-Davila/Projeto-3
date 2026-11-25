package com.wxn.backend.fila.servico;

import java.time.LocalDateTime;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

// Coluna "Meus atendimentos": conversas que o atendente logado já assumiu.
public record ItemAtendimento(
		Long conversaId,
		String nomeCliente,
		String telefoneCliente,
		String assunto,
		String equipe,
		StatusConversa status,
		Long atendenteId,
		String nomeAtendente,
		LocalDateTime entrouNaFilaEm) {

	static ItemAtendimento de(Conversa conversa) {
		Atendente atendente = conversa.getAtendente();
		return new ItemAtendimento(
				conversa.getId(),
				conversa.getCliente().getNome(),
				conversa.getTelefoneCliente(),
				conversa.getAssunto(),
				conversa.getEquipe(),
				conversa.getStatus(),
				atendente != null ? atendente.getId() : null,
				atendente != null ? atendente.getNome() : null,
				conversa.getEntrouNaFilaEm());
	}
}
