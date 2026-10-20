package com.wxn.backend.fila.dominio;

// Ciclo de vida da conversa. Persistido como texto (EnumType.STRING) para que
// reordenar ou inserir constantes no futuro não corrompa os dados já gravados.
public enum StatusConversa {
	EM_ATENDIMENTO_BOT,
	AGUARDANDO_HUMANO,
	EM_ATENDIMENTO_HUMANO,
	ENCERRADA
}
