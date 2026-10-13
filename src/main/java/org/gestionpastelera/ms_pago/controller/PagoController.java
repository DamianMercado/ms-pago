package org.gestionpastelera.ms_pago.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gestionpastelera.ms_pago.dto.request.AnularPagoRequest;
import org.gestionpastelera.ms_pago.dto.request.PagoRequest;
import org.gestionpastelera.ms_pago.dto.response.PagoResponse;
import org.gestionpastelera.ms_pago.dto.response.ResumenCajaResponse;
import org.gestionpastelera.ms_pago.enums.MetodoPago;
import org.gestionpastelera.ms_pago.service.PagoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','CAJERO')")
        public ResponseEntity<List<PagoResponse>> listarPagos(
            @RequestParam(required = false) MetodoPago metodo
    ) {
        return ResponseEntity.ok(pagoService.listarPagos(metodo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<PagoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id));
    }

    @GetMapping("/venta/{ventaId}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<List<PagoResponse>> obtenerPorVentaId(@PathVariable Long ventaId) {
        return ResponseEntity.ok(pagoService.obtenerPagosPorVentaId(ventaId));
    }

    @GetMapping("/comprobante/{numeroComprobante}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<PagoResponse> obtenerPorComprobante(@PathVariable String numeroComprobante) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorComprobante(numeroComprobante));
    }

    @GetMapping("/cliente/{rut}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO') or #rut == authentication.name")
        public ResponseEntity<List<PagoResponse>> obtenerPorCliente(@PathVariable String rut) {
        return ResponseEntity.ok(pagoService.obtenerPagosPorCliente(rut));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<PagoResponse> crearPago(
            @Valid @RequestBody PagoRequest request,
            Authentication authentication
    ) {
        String cajeroRut = authentication != null ? authentication.getName() : "SISTEMA";
        PagoResponse response = pagoService.crearPago(request, cajeroRut);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<PagoResponse> anularPago(
            @PathVariable Long id,
            @Valid @RequestBody AnularPagoRequest request
    ) {
        return ResponseEntity.ok(pagoService.anularPago(id, request));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<ResumenCajaResponse> obtenerResumenCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(pagoService.obtenerResumenCaja(fecha));
    }
}
