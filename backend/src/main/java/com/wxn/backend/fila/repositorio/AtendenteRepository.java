package com.wxn.backend.fila.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.backend.fila.dominio.Atendente;

public interface AtendenteRepository extends JpaRepository<Atendente, Long> {

	List<Atendente> findAllByOrderByNomeAsc();
}
