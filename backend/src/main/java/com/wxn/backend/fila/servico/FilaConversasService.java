package com.wxn.backend.fila.servico;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.backend.fila.dominio.Atendente;
import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;
import com.wxn.backend.fila.repositorio.AtendenteRepository;
import com.wxn.backend.fila.repositorio.ConversaRepository;

@Service
public class FilaConversasService {

	private final ConversaRepository conversas;
	private final AtendenteRepository atendentes;

	public FilaConversasService(ConversaRepository conversas, AtendenteRepository atendentes) {
		this.conversas = conversas;
		this.atendentes = atendentes;
	}

	@Transactional(readOnly = true)
	public List<ItemFila> listarFila() {
		LocalDateTime agora = LocalDateTime.now();
		return conversas.findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa.AGUARDANDO_HUMANO)
				.stream()
				.map(conversa -> ItemFila.de(conversa, agora))
				.toList();
	}

	@Transactional(readOnly = true)
	public Conversa buscarPorId(Long id) {
		return conversas.findById(id)
				.orElseThrow(() -> new ConversaNaoEncontradaException(id));
	}

	@Transactional
	public Conversa assumir(Long conversaId, Long atendenteId) {
		Conversa conversa = buscarPorId(conversaId);
		Atendente atendente = atendentes.findById(atendenteId)
				.orElseThrow(() -> new AtendenteNaoEncontradoException(atendenteId));

		if (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO_HUMANO) {
			throw new ConversaJaAssumidaException(conversaId);
		}
		conversa.assumirPor(atendente);

		try {
			// Clique simultâneo: o @Version rejeita o segundo UPDATE. O flush antecipa esse erro
			// para dentro do método, senão ele só apareceria no commit.
			return conversas.saveAndFlush(conversa);
		} catch (OptimisticLockingFailureException e) {
			throw new ConversaJaAssumidaException(conversaId);
		}
	}

}
