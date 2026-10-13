package org.gestionpastelera.ms_pago.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestionpastelera.ms_pago.client.VentaClient;
import org.gestionpastelera.ms_pago.client.dto.ActualizarEstadoVentaClientRequest;
import org.gestionpastelera.ms_pago.client.dto.VentaClientResponse;
import org.gestionpastelera.ms_pago.dto.request.AnularPagoRequest;
import org.gestionpastelera.ms_pago.dto.request.PagoRequest;
import org.gestionpastelera.ms_pago.dto.response.PagoResponse;
import org.gestionpastelera.ms_pago.dto.response.ResumenCajaResponse;
import org.gestionpastelera.ms_pago.enums.EstadoPago;
import org.gestionpastelera.ms_pago.enums.MetodoPago;
import org.gestionpastelera.ms_pago.mapper.PagoMapper;
import org.gestionpastelera.ms_pago.model.Pago;
import org.gestionpastelera.ms_pago.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;
    private final VentaClient ventaClient;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos(MetodoPago metodo) {
        List<Pago> pagos;
        if (metodo != null) {
            pagos = pagoRepository.findByMetodoPago(metodo);
        } else {
            pagos = pagoRepository.findAll();
        }
        return pagoMapper.toResponseList(pagos);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pago con ID: " + id));
        return pagoMapper.toResponse(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> obtenerPagosPorVentaId(Long ventaId) {
        List<Pago> pagos = pagoRepository.findByVentaId(ventaId);
        if (pagos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron pagos asociados a la venta #" + ventaId);
        }
        return pagoMapper.toResponseList(pagos);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPagoPorComprobante(String numeroComprobante) {
        Pago pago = pagoRepository.findByNumeroComprobante(numeroComprobante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pago con comprobante: " + numeroComprobante));
        return pagoMapper.toResponse(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> obtenerPagosPorCliente(String clienteRut) {
        List<Pago> pagos = pagoRepository.findByClienteRutOrderByFechaPagoDesc(clienteRut);
        return pagoMapper.toResponseList(pagos);
    }

    @Transactional
    public PagoResponse crearPago(PagoRequest request, String cajeroRut) {
        // Validar que no exista un pago activo (PAGADO) para esta venta
        Optional<Pago> pagoExistente = pagoRepository.findByVentaIdAndEstadoPago(request.getVentaId(), EstadoPago.PAGADO);
        if (pagoExistente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un pago completado para la venta #" + request.getVentaId());
        }

        // Obtener venta remota desde ms-venta
        VentaClientResponse venta;
        try {
            venta = ventaClient.obtenerVentaPorId(request.getVentaId());
        } catch (FeignException e) {
            throw translateFeignException(e, "consultar venta");
        }

        if (venta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta con ID: " + request.getVentaId());
        }

        // Exigir estado PENDIENTE_PAGO
        if (!"PENDIENTE_PAGO".equalsIgnoreCase(venta.getEstadoVenta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                    "Solo se pueden registrar pagos para ventas en estado PENDIENTE_PAGO. El estado actual de la venta #%d es %s",
                    request.getVentaId(), venta.getEstadoVenta()
            ));
        }

        // Exigir monto exacto
        if (request.getMonto().compareTo(venta.getTotalVenta()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                    "El monto pagado ($%s) no coincide con el total requerido de la venta ($%s)",
                    request.getMonto(), venta.getTotalVenta()
            ));
        }

        // Generar número de comprobante único: CMP-yyyyMMdd-XXXXXXXX
        String fechaStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidStr = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String numeroComprobante = "CMP-" + fechaStr + "-" + uuidStr;

        Pago pago = Pago.builder()
                .ventaId(request.getVentaId())
                .clienteRut(venta.getClienteRut())
                .clienteNombre(venta.getClienteNombre())
                .monto(request.getMonto())
                .metodoPago(request.getMetodoPago())
                .estadoPago(EstadoPago.PAGADO)
                .numeroComprobante(numeroComprobante)
                .cajeroRut(cajeroRut)
                .fechaPago(LocalDateTime.now())
                .notas(request.getNotas() != null ? request.getNotas().trim() : null)
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        // Actualizar estado de la venta a PAGADA en ms-venta (si falla, rollback del pago)
        try {
            ventaClient.actualizarEstadoVenta(
                    request.getVentaId(),
                    ActualizarEstadoVentaClientRequest.builder().estado("PAGADA").build()
            );
        } catch (FeignException e) {
            throw translateFeignException(e, "actualizar estado de venta");
        }

        log.info("Pago #{} registrado exitosamente para la venta #{} con comprobante {}",
                pagoGuardado.getId(), request.getVentaId(), numeroComprobante);

        return pagoMapper.toResponse(pagoGuardado);
    }

    @Transactional
    public PagoResponse anularPago(Long id, AnularPagoRequest request) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pago con ID: " + id));

        if (pago.getEstadoPago() == EstadoPago.ANULADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago #" + id + " ya se encuentra ANULADO");
        }

        pago.setEstadoPago(EstadoPago.ANULADO);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setMotivoAnulacion(request.getMotivo().trim());

        Pago pagoActualizado = pagoRepository.save(pago);

        // Revertir estado de la venta a PENDIENTE_PAGO en ms-venta
        try {
            ventaClient.actualizarEstadoVenta(
                    pago.getVentaId(),
                    ActualizarEstadoVentaClientRequest.builder().estado("PENDIENTE_PAGO").build()
            );
        } catch (FeignException e) {
            throw translateFeignException(e, "revertir estado de venta tras anulación");
        }

        log.info("Pago #{} anulado con éxito por motivo: {}", id, request.getMotivo());
        return pagoMapper.toResponse(pagoActualizado);
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumenCaja(LocalDate fecha) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        LocalDateTime inicio = fechaConsulta.atStartOfDay();
        LocalDateTime fin = fechaConsulta.plusDays(1).atStartOfDay();

        List<Object[]> resultados = pagoRepository.obtenerTotalesPorMetodoYPeriodo(
                EstadoPago.PAGADO,
                inicio,
                fin
        );

        Map<String, BigDecimal> totalesPorMetodo = new LinkedHashMap<>();
        for (MetodoPago metodo : MetodoPago.values()) {
            totalesPorMetodo.put(metodo.name(), BigDecimal.ZERO.setScale(2));
        }

        long cantidadTotal = 0L;
        BigDecimal totalRecaudado = BigDecimal.ZERO;

        for (Object[] row : resultados) {
            MetodoPago metodo = (MetodoPago) row[0];
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal monto = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            totalesPorMetodo.put(metodo.name(), monto);
            cantidadTotal += count;
            totalRecaudado = totalRecaudado.add(monto);
        }

        return ResumenCajaResponse.builder()
                .fecha(fechaConsulta.toString())
                .cantidadPagos(cantidadTotal)
                .totalRecaudado(totalRecaudado)
                .totalesPorMetodo(totalesPorMetodo)
                .build();
    }

    private RuntimeException translateFeignException(FeignException e, String operacion) {
        int status = e.status();
        if (status == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta en el microservicio de ventas");
        }
        if (status == 400 || status == 409) {
            String body = e.contentUTF8();
            if (body != null) {
                Matcher matcher = MESSAGE_PATTERN.matcher(body);
                if (matcher.find()) {
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, matcher.group(1));
                }
            }
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error en servicio de ventas: " + e.getMessage());
        }
        if (status == 401 || status == 403) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El servicio de ventas rechazó la autorización del usuario actual");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de ventas no está disponible para " + operacion + ": " + e.getMessage());
    }
}
