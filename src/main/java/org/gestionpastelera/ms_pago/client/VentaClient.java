package org.gestionpastelera.ms_pago.client;

import org.gestionpastelera.ms_pago.client.dto.ActualizarEstadoVentaClientRequest;
import org.gestionpastelera.ms_pago.client.dto.VentaClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-venta", url = "${services.venta.url}")
public interface VentaClient {

    @GetMapping("/api/v1/ventas/{id}")
    VentaClientResponse obtenerVentaPorId(@PathVariable("id") Long id);

    @PatchMapping("/api/v1/ventas/{id}/estado")
    void actualizarEstadoVenta(@PathVariable("id") Long id, @RequestBody ActualizarEstadoVentaClientRequest request);
}
