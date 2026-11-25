package com.wxn.backend.fila.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.backend.fila.dominio.Conversa;
import com.wxn.backend.fila.dominio.StatusConversa;

/**
 * Acesso às conversas persistidas. A implementação é gerada pelo Spring Data.
 * <p>
 * A "fila" não é armazenada em lugar nenhum: ela é o filtro por {@code status}
 * mais a ordem por {@code entrouNaFilaEm} (mais antiga primeiro).
 */
public interface ConversaRepository extends JpaRepository<Conversa, Long> {

	// EntityGraph: traz o cliente na mesma consulta, evitando um SELECT extra por item (N+1).
	@EntityGraph(attributePaths = "cliente")
	List<Conversa> findByStatusOrderByEntrouNaFilaEmAscIdAsc(StatusConversa status);
}
