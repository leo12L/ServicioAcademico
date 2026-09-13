package com.escuelita.sistemaacademico.controller;

import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @GetMapping("/lista-espera")
    public List<Usuario> listaEspera() {
        return usuarioService.listarListaEspera();
    }

    @GetMapping("/administrador")
    public Usuario administrador() {
        return usuarioService.obtenerAdministrador();
    }

    @PostMapping
    public Usuario crear(@Valid @RequestBody Usuario usuario) {
        return usuarioService.crearPorAdmin(usuario);
    }

    @PutMapping("/{id}")
    public Usuario actualizar(@PathVariable Long id, @Valid @RequestBody Usuario usuario) {
        usuario.setId(id);
        return usuarioService.guardar(usuario);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    @PatchMapping("/{id}/activar")
    public Usuario activar(@PathVariable Long id) {
        return usuarioService.activar(id);
    }

    @PatchMapping("/{id}/rechazar")
    public Usuario rechazar(@PathVariable Long id) {
        return usuarioService.rechazar(id);
    }

    @PatchMapping("/{id}/telefono")
    public Usuario actualizarTelefono(@PathVariable Long id, @RequestParam String telefono) {
        return usuarioService.actualizarTelefono(id, telefono);
    }
}
