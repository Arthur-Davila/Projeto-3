package com.wxn.backend.fila.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wxn.backend.fila.servico.AtendenteResumo;
import com.wxn.backend.fila.servico.DetalheConversa;
import com.wxn.backend.fila.servico.FilaConversasService;
import com.wxn.backend.fila.servico.ItemAtendimento;

// Camada de apresentação HTTP da segunda história: fila, assunção e
// acompanhamento das conversas em atendimento humano.
@RestController
@RequestMapping("/api")
public class FilaController {

	private final FilaConversasService servico;

	public FilaController(FilaConversasService servico) {
		this.servico = servico;
	}

	@GetMapping("/fila")
	public List<ItemFilaResposta> listarFila() {
		return servico.listarFila()
				.stream()
				.map(ItemFilaResposta::de)
				.toList();
	}

	@GetMapping("/atendentes")
	public List<AtendenteResumo> listarAtendentes() {
		return servico.listarAtendentes();
	}

	@GetMapping("/atendimentos")
	public List<ItemAtendimento> listarAtendimentos(@RequestParam Long atendenteId) {
		return servico.listarAtendimentosDoAtendente(atendenteId);
	}

	@GetMapping("/conversas/{conversaId}")
	public DetalheConversa detalhar(@PathVariable Long conversaId) {
		return servico.detalhar(conversaId);
	}

	@PostMapping("/fila/{conversaId}/assumir")
	public DetalheConversa assumir(
			@PathVariable Long conversaId,
			@RequestBody AssumirConversaRequest requisicao) {
		if (requisicao == null || requisicao.atendenteId() == null) {
			throw new IllegalArgumentException("Atendente é obrigatório");
		}
		servico.assumir(conversaId, requisicao.atendenteId());
		return servico.detalhar(conversaId);
	}

	@PostMapping("/conversas/{conversaId}/mensagens")
	public DetalheConversa enviarMensagem(
			@PathVariable Long conversaId,
			@RequestBody EnviarMensagemRequest requisicao) {
		if (requisicao == null || requisicao.atendenteId() == null) {
			throw new IllegalArgumentException("Atendente é obrigatório");
		}
		return servico.enviarMensagem(conversaId, requisicao.atendenteId(), requisicao.conteudo());
	}
}
