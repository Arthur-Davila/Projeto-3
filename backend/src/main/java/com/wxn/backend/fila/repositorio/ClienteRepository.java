package com.wxn.backend.fila.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.backend.fila.dominio.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByTelefone(String telefone);
}
