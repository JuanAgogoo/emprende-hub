package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Cuenta de acceso, sea de cliente, emprendedor o administrador.
 *
 * <p>Una sola tabla con un rol, no una por perfil (decisión A): los tres pueden
 * opinar, y un cliente puede registrar un negocio más adelante sin abrir una
 * segunda cuenta (A1-bis), lo que solo requiere cambiarle el rol.
 *
 * <p>Implementa {@link UserDetails} para enchufar directamente con Spring
 * Security, como en el taller del curso.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Identifica la cuenta y sirve para entrar. Nunca se muestra en público. */
    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    /** Siempre con hash BCrypt. Aquí no entra nunca una contraseña en claro. */
    @Column(nullable = false, length = 100)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Suspensión por el administrador (B4). Un suspendido no puede entrar. */
    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private Instant fechaRegistro;

    public Usuario(String nombre, String correo, String contrasena, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
        this.activo = true;
        this.fechaRegistro = Instant.now();
    }

    // ---------- Contrato de UserDetails ----------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.comoAutoridad()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    /** Para Spring Security el nombre de usuario es el correo. */
    @Override
    public String getUsername() {
        return correo;
    }

    /**
     * Una cuenta suspendida queda deshabilitada, y Spring Security la rechaza
     * en el momento de autenticar sin que haya que comprobarlo en cada sitio.
     */
    @Override
    public boolean isEnabled() {
        return activo;
    }
}
