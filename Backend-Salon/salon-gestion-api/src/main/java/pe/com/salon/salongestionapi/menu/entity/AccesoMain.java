package pe.com.salon.salongestionapi.menu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acceso_main")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
}
