package org.gestionpastelera.ms_pago.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnularPagoRequest {

    @NotBlank(message = "El motivo de la anulación es obligatorio")
        private String motivo;
}
