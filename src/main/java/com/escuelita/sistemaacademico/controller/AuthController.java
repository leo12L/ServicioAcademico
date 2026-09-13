package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.dto.LoginRequest;
import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/api/login")
    public Usuario login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }
}
