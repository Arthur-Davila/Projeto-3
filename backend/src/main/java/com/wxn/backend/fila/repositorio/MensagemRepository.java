package com.wxn.backend.fila.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.backend.fila.dominio.Mensagem;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

	boolean existsByWhatsappMessageId(String whatsappMessageId);
}
