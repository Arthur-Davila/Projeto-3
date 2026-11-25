package com.wxn.backend.fila.servico;

import java.time.LocalDateTime;
import java.util.List;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

// Visão completa usada no modal de histórico e como resposta da assunção.
public record DetalheConversa(
		Long conversaId,
		String nomeCliente,
		String telefoneCliente,
		String assunto,
		String equipe,
		StatusConversa status,
		Long atendenteId,
		String nomeAtendente,
		LocalDateTime entrouNaFilaEm,
		List<MensagemResumo> mensagens) {

	static DetalheConversa de(Conversa conversa) {
		Atendente atendente = conversa.getAtendente();
		return new DetalheConversa(
				conversa.getId(),
				conversa.getCliente().getNome(),
				conversa.getTelefoneCliente(),
				conversa.getAssunto(),
				conversa.getEquipe(),
				conversa.getStatus(),
				atendente != null ? atendente.getId() : null,
				atendente != null ? atendente.getNome() : null,
				conversa.getEntrouNaFilaEm(),
				conversa.getMensagens().stream().map(MensagemResumo::de).toList());
	}
}
