package com.wxn.backend.fila.servico;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;
import com.wxn.backend.fila.repositorio.ConversaRepository;

// Coordena, não guarda regra: busca no repositório, pede para a Conversa agir
// (ela valida a transição) e salva. Não existe classe Fila: a fila é filtro + ordem.
@Service
public class FilaConversasService {

	// Depende da interface, não da implementação: o Spring injeta a versão disponível.
	private final ConversaRepository repositorio;

	public FilaConversasService(ConversaRepository repositorio) {
		this.repositorio = repositorio;
	}

	// Conversas aguardando humano, quem esperou mais primeiro. Quando o status muda,
	// a conversa sai da fila sozinha.
	public List<Conversa> listarFila() {
		return repositorio.buscarPorStatusOrdenadoPorEntrada(StatusConversa.AGUARDANDO_HUMANO);
	}

	public Conversa buscarPorId(Long id) {
		return repositorio.buscarPorId(id)
				.orElseThrow(() -> new ConversaNaoEncontradaException(id));
	}

	// A regra "pode ser assumida?" fica na Conversa. Quem garante que só um atendente
	// vence um clique simultâneo é o banco (@Version na Conversa), dentro desta transação;
	// a versão em memória não oferece essa garantia.
	@Transactional
	public Conversa assumir(Long conversaId, Long atendenteId) {
		Conversa conversa = buscarPorId(conversaId);
		conversa.assumirPor(atendenteId);
		return repositorio.salvar(conversa);
	}

}
