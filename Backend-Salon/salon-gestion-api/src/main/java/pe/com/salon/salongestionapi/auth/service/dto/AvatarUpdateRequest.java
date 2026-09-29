package pe.com.salon.salongestionapi.auth.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUpdateRequest {
    @NotBlank(message = "El avatarUrl es obligatorio")
    private String avatarUrl;
}
