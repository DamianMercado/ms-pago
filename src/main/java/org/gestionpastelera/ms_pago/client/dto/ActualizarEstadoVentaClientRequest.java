package org.gestionpastelera.ms_pago.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarEstadoVentaClientRequest {
    private String estado;
}
