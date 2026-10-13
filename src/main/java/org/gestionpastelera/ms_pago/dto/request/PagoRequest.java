package org.gestionpastelera.ms_pago.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.gestionpastelera.ms_pago.enums.MetodoPago;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoRequest {

    @NotNull(message = "El ID de la venta es obligatorio")
        private Long ventaId;

    @NotNull(message = "El método de pago es obligatorio")
        private MetodoPago metodoPago;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto pagado debe ser mayor a 0")
        private BigDecimal monto;

        private String notas;
}
