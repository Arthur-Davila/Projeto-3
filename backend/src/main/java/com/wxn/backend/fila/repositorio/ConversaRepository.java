package com.wxn.backend.fila.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	// Conversas que já saíram da fila e estão com um atendente humano.
	@EntityGraph(attributePaths = { "cliente", "atendente" })
	@Query("""
			select c from Conversa c
			where c.atendente.id = :atendenteId and c.status = :status
			order by c.entrouNaFilaEm asc, c.id asc
			""")
	List<Conversa> buscarAtendimentosDoAtendente(
			@Param("atendenteId") Long atendenteId,
			@Param("status") StatusConversa status);

	@EntityGraph(attributePaths = { "cliente", "atendente", "mensagens" })
	Optional<Conversa> findFirstByCliente_TelefoneAndStatusInOrderByIdDesc(
			String telefone,
			Collection<StatusConversa> status);

	// Detalhe do painel: cliente, atendente e histórico de mensagens em uma única consulta.
	@EntityGraph(attributePaths = { "cliente", "atendente", "mensagens" })
	@Query("select c from Conversa c where c.id = :id")
	Optional<Conversa> findComDetalhesById(@Param("id") Long id);
}
