package com.escuelita.sistemaacademico.repository;

import com.escuelita.sistemaacademico.model.Rol;
import com.escuelita.sistemaacademico.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRol(Rol rol);
}
