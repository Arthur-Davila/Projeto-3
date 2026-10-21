package com.wxn.backend.fila.repositorio;

import java.util.List;
import java.util.Optional;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

// Contrato de persistência. O serviço depende só desta interface, então trocar a versão
// em memória pela de banco não exige mudar nenhuma linha do serviço.
public interface ConversaRepository {

	// Grava a conversa e devolve a instância salva (já com id, se era nova).
	Conversa salvar(Conversa conversa);

	// Optional deixa explícito que o id pode não existir; quem chama decide o que fazer.
	Optional<Conversa> buscarPorId(Long id);

	// Ordem FIFO: quem entrou na fila primeiro (entrouNaFilaEm mais antigo) vem primeiro.
	// A ordenação é responsabilidade do repositório para, no banco, virar um ORDER BY.
	List<Conversa> buscarPorStatusOrdenadoPorEntrada(StatusConversa status);

}
