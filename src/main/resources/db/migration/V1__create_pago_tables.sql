CREATE TABLE pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    cliente_rut VARCHAR(12) NOT NULL,
    cliente_nombre VARCHAR(150) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL,
    numero_comprobante VARCHAR(40) NOT NULL UNIQUE,
    cajero_rut VARCHAR(12) NOT NULL,
    fecha_pago DATETIME NOT NULL,
    fecha_anulacion DATETIME NULL,
    motivo_anulacion VARCHAR(255),
    notas VARCHAR(500)
);

CREATE INDEX idx_pago_venta_id ON pago (venta_id);
CREATE INDEX idx_pago_cliente_rut ON pago (cliente_rut);
