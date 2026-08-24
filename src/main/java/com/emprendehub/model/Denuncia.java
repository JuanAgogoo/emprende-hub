package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aviso de que una opinión no debería estar publicada (C3).
 *
 * <p>Es la única vía por la que una opinión llega al administrador: se publican
 * al instante y solo se moderan si alguien las denuncia (C4).
 *
 * <p>Denuncia cualquier usuario con sesión, incluido el dueño del negocio
 * afectado. La restricción de unicidad impide que la misma persona denuncie dos
 * veces la misma opinión, que solo abultaría la cola sin aportar nada.
 *
 * <p>Esta pantalla y este botón <strong>no existen en el prototipo</strong>.
 */
@Entity
@Table(name = "denuncia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_denuncia_opinion_denunciante",
                columnNames = {"opinion_id", "denunciante_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opinion_id", nullable = false)
    private Opinion opinion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MotivoDenuncia motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDenuncia estado;

    @Column(nullable = false)
    private Instant fecha;

    public Denuncia(Opinion opinion, Usuario denunciante, MotivoDenuncia motivo) {
        this.opinion = opinion;
        this.denunciante = denunciante;
        this.motivo = motivo;
        this.estado = EstadoDenuncia.PENDIENTE;
        this.fecha = Instant.now();
    }
}
