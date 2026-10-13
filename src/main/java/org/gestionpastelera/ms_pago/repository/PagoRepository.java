package org.gestionpastelera.ms_pago.repository;

import org.gestionpastelera.ms_pago.enums.EstadoPago;
import org.gestionpastelera.ms_pago.enums.MetodoPago;
import org.gestionpastelera.ms_pago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByVentaIdAndEstadoPago(Long ventaId, EstadoPago estadoPago);

    List<Pago> findByVentaId(Long ventaId);

    Optional<Pago> findByNumeroComprobante(String numeroComprobante);

    List<Pago> findByMetodoPago(MetodoPago metodoPago);

    List<Pago> findByClienteRutOrderByFechaPagoDesc(String clienteRut);

    @Query("SELECT p.metodoPago, COUNT(p), COALESCE(SUM(p.monto), 0) FROM Pago p " +
           "WHERE p.estadoPago = :estado AND p.fechaPago >= :inicio AND p.fechaPago < :fin " +
           "GROUP BY p.metodoPago")
    List<Object[]> obtenerTotalesPorMetodoYPeriodo(
            @Param("estado") EstadoPago estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}
