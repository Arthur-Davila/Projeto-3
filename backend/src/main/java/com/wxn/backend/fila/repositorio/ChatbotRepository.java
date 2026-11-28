package com.wxn.backend.fila.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.backend.fila.dominio.Chatbot;

public interface ChatbotRepository extends JpaRepository<Chatbot, Long> {

	Optional<Chatbot> findFirstByAtivoTrueOrderByIdAsc();
}
