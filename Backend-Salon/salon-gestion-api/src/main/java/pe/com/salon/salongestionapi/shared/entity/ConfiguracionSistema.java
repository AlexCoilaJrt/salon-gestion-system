package pe.com.salon.salongestionapi.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionSistema {

    @Id
    @Column(name = "clave", nullable = false, unique = true, length = 80)
    private String clave;

    @Column(name = "valor", nullable = false, length = 255)
    private String valor;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
