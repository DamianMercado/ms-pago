package org.gestionpastelera.ms_pago.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaClientResponse {
    private Long id;
    private String clienteRut;
    private String clienteNombre;
    private BigDecimal totalVenta;
    private String estadoVenta;
}
