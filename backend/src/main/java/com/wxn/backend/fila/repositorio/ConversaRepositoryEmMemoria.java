package com.wxn.backend.fila.repositorio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

// Implementação provisória que destrava o serviço enquanto a versão de banco não existe.
// Os dados vivem só enquanto a aplicação está no ar.
@Repository
public class ConversaRepositoryEmMemoria implements ConversaRepository {

	// Mais antigas primeiro; quem nunca entrou na fila (null) vai para o fim.
	// Empate no horário desempata pelo id, para a ordem não depender do HashMap.
	private static final Comparator<Conversa> ORDEM_DE_ENTRADA = Comparator
			.comparing(Conversa::getEntrouNaFilaEm, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Conversa::getId);

	// Concorrentes porque vários atendentes acessam a fila ao mesmo tempo.
	private final Map<Long, Conversa> conversas = new ConcurrentHashMap<>();
	private final AtomicLong proximoId = new AtomicLong(1);

	@Override
	public Conversa salvar(Conversa conversa) {
		if (conversa == null) {
			throw new IllegalArgumentException("Conversa é obrigatória");
		}
		// Faz o papel do @GeneratedValue do banco.
		if (conversa.getId() == null) {
			conversa.atribuirId(proximoId.getAndIncrement());
		}
		conversas.put(conversa.getId(), conversa);
		return conversa;
	}

	@Override
	public Optional<Conversa> buscarPorId(Long id) {
		// ConcurrentHashMap lança NullPointerException com chave nula.
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(conversas.get(id));
	}

	@Override
	public List<Conversa> buscarPorStatusOrdenadoPorEntrada(StatusConversa status) {
		return conversas.values().stream()
				.filter(conversa -> conversa.getStatus() == status)
				.sorted(ORDEM_DE_ENTRADA)
				.toList();
	}

}
