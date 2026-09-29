package pe.com.salon.salongestionapi.empresa.dto;

public record EmpresaResponse(
    Long id,
    String nombreComercial,
    String razonSocial,
    String ruc,
    String direccion,
    String telefono,
    String email,
    String logoUrl
) {
}
