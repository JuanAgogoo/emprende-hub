package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permiso de un solo uso para cambiar la contraseña sin saber la anterior (I1).
 *
 * <p>El token se guarda <strong>tal cual, sin cifrar</strong>, y es una
 * simplificación consciente: quien pueda leer esta tabla ya tiene la de
 * usuarios. Lo que acota su valor es que solo sirve una vez y que caduca a los
 * treinta minutos.
 *
 * <p>No se borra al usarlo, se marca: así el segundo intento con el mismo
 * enlace puede responder «este enlace ya no vale» en vez de «no existe».
 */
@Entity
@Table(name = "token_recuperacion")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacion {

    /** 32 bytes al azar en Base64 sin relleno ocupan 43 caracteres. */
    public static final int LONGITUD_TOKEN = 43;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** De quién es la cuenta que se va a recuperar. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = LONGITUD_TOKEN)
    private String token;

    @Column(nullable = false)
    private Instant caducidad;

    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private Instant fechaCreacion;

    public TokenRecuperacion(Usuario usuario, String token, Instant caducidad) {
        this.usuario = usuario;
        this.token = token;
        this.caducidad = caducidad;
        this.usado = false;
        this.fechaCreacion = Instant.now();
    }

    /** Sirve si nadie lo ha usado todavía y no se le ha pasado la hora. */
    public boolean sirve(Instant momento) {
        return !usado && caducidad.isAfter(momento);
    }
}
