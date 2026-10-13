package org.gestionpastelera.ms_pago.dto.response;

import lombok.*;
import org.gestionpastelera.ms_pago.enums.EstadoPago;
import org.gestionpastelera.ms_pago.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoResponse {

        private Long id;

        private Long ventaId;

        private String clienteRut;

        private String clienteNombre;

        private BigDecimal monto;

        private MetodoPago metodoPago;

        private EstadoPago estadoPago;

        private String numeroComprobante;

        private String cajeroRut;

        private LocalDateTime fechaPago;

        private LocalDateTime fechaAnulacion;

        private String motivoAnulacion;

        private String notas;
}
