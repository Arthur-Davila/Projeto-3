package com.wxn.backend.fila.servico;

import com.wxn.backend.fila.dominio.Atendente;

// O que o painel precisa para montar o seletor de atendente logado.
public record AtendenteResumo(
		Long id,
		String nome,
		String email,
		String equipe,
		boolean disponivel) {

	static AtendenteResumo de(Atendente atendente) {
		return new AtendenteResumo(
				atendente.getId(),
				atendente.getNome(),
				atendente.getEmail(),
				atendente.getEquipe(),
				atendente.isDisponivel());
	}
}
