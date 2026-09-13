package com.escuelita.sistemaacademico.service;

import com.escuelita.sistemaacademico.dto.LoginRequest;
import com.escuelita.sistemaacademico.dto.RegistroRequest;
import com.escuelita.sistemaacademico.exception.CredencialesInvalidasException;
import com.escuelita.sistemaacademico.model.EstadoCuenta;
import com.escuelita.sistemaacademico.model.Rol;
import com.escuelita.sistemaacademico.model.Usuario;
import com.escuelita.sistemaacademico.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarListaEspera() {
        return usuarioRepository.findByEstado(EstadoCuenta.LISTA_ESPERA);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    public Usuario obtenerAdministrador() {
        return usuarioRepository.findByRol(Rol.ADMINISTRADOR).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay administrador registrado"));
    }

    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    /**
     * Alta desde el CRUD de administrador (a diferencia de registrar(), aquí sí se
     * elige rol/estado libremente) — hashea el password igual que el registro público.
     */
    public Usuario crearPorAdmin(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    /**
     * Alta pública desde la landing: siempre entra como ALUMNO en LISTA_ESPERA,
     * sin importar lo que venga en el request — el rol y el estado no son elegibles por el usuario.
     */
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(Rol.ALUMNO);
        usuario.setEstado(EstadoCuenta.LISTA_ESPERA);
        return usuarioRepository.save(usuario);
    }

    public Usuario activar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setEstado(EstadoCuenta.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    public Usuario rechazar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setEstado(EstadoCuenta.RECHAZADO);
        return usuarioRepository.save(usuario);
    }

    public Usuario actualizarTelefono(Long id, String telefono) {
        Usuario usuario = buscarPorId(id);
        usuario.setTelefono(telefono);
        return usuarioRepository.save(usuario);
    }

    public Usuario login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }
        if (usuario.getEstado() == EstadoCuenta.LISTA_ESPERA) {
            throw new CredencialesInvalidasException("Tu cuenta sigue en lista de espera, aún no está activada");
        }
        if (usuario.getEstado() == EstadoCuenta.RECHAZADO) {
            throw new CredencialesInvalidasException("Tu cuenta fue rechazada, contacta a la administración");
        }
        return usuario;
    }
}
