ALTER TABLE asignaciones_personal
    ADD INDEX idx_asignaciones_personal_estado (estado),
    ADD INDEX idx_asignaciones_personal_personal_fecha_estado (id_personal, fecha_asignacion, estado);
