    CREATE TABLE IF NOT EXISTS reserva_items (
        id_reserva_item INT AUTO_INCREMENT PRIMARY KEY,
        id_reserva INT NOT NULL,
        tipo_item ENUM('SERVICIO','PAQUETE') NOT NULL,
        id_servicio INT NULL,
        id_paquete INT NULL,
        cantidad INT NOT NULL DEFAULT 1,
        precio_unitario DECIMAL(10,2) NOT NULL,
        precio_total DECIMAL(10,2) NOT NULL,
        descripcion_extra VARCHAR(500) NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT fk_reserva_items_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva),
        CONSTRAINT fk_reserva_items_servicio FOREIGN KEY (id_servicio) REFERENCES servicios_turisticos(id_servicio),
        CONSTRAINT fk_reserva_items_paquete FOREIGN KEY (id_paquete) REFERENCES paquetes_turisticos(id_paquete)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE INDEX idx_reserva_items_reserva ON reserva_items (id_reserva);
    CREATE INDEX idx_reserva_items_servicio ON reserva_items (id_servicio);
    CREATE INDEX idx_reserva_items_paquete ON reserva_items (id_paquete);

INSERT INTO reserva_items (
    id_reserva,
    tipo_item,
    id_servicio,
    id_paquete,
    cantidad,
    precio_unitario,
    precio_total,
    descripcion_extra,
    created_at,
    updated_at
)
SELECT
    r.id_reserva,
    CASE WHEN r.id_servicio IS NOT NULL THEN 'SERVICIO' ELSE 'PAQUETE' END,
    r.id_servicio,
    r.id_paquete,
    1,
    COALESCE(r.precio_total, 0.00),
    COALESCE(r.precio_total, 0.00),
    'Migración inicial',
    r.created_at,
    r.updated_at
FROM reservas r
WHERE (r.id_servicio IS NOT NULL OR r.id_paquete IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM reserva_items ri WHERE ri.id_reserva = r.id_reserva
  );

UPDATE reserva_items
SET precio_unitario = precio_total
WHERE precio_unitario IS NULL;
