package org.gestionpastelera.ms_pago.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenCajaResponse {

        private String fecha;

        private Long cantidadPagos;

        private BigDecimal totalRecaudado;

        private Map<String, BigDecimal> totalesPorMetodo;
}
