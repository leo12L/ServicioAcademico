package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.dto.RegistroRequest;
import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/api/registro")
    public Usuario registrar(@Valid @RequestBody RegistroRequest request) {
        return usuarioService.registrar(request);
    }
}
