-- Agregar columna id_sucursal a la tabla clientes
ALTER TABLE clientes
    ADD COLUMN id_sucursal INT(11) NULL AFTER id_empresa,
    ADD INDEX idx_clientes_sucursal (id_sucursal),
    ADD CONSTRAINT fk_clientes_sucursal 
        FOREIGN KEY (id_sucursal) 
        REFERENCES sucursales(id_sucursal) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE;

