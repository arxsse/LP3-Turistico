# Checklist de Pruebas del Módulo de Reportes

Este documento sirve para coordinar las verificaciones manuales del módulo `inicio/REPORTES/`. Cada sección incluye pasos sugeridos, el rol recomendado y un espacio para registrar el resultado.

## 1. Escenarios multiempresa

| Caso | Rol sugerido | Pasos | Resultado |
| --- | --- | --- | --- |
| Empresa propia | Administrador | Iniciar sesión como administrador con empresa asignada. Abrir Reportes y validar que no se muestra selector de empresas y los datos corresponden a su empresa. | |
| Selección de empresa | Superadmin | Iniciar sesión como superadmin, escoger empresa A desde el selector, refrescar y documentar los KPIs. Repetir con empresa B. | |
| Persistencia de empresa | Superadmin | Seleccionar empresa distinta, navegar fuera de Reportes y volver. Confirmar que se conserva la selección y el dataset refleja la empresa guardada en `sessionStorage`. | |

## 2. Filtros y refresco

| Caso | Pasos | Resultado |
| --- | --- | --- |
| Rango válido | Cambiar fechas a un rango de 30 días anteriores, pulsar Actualizar, validar que todos los gráficos/tables se regeneran sin errores. | |
| Rango inválido | Configurar fecha inicio mayor a fecha fin. Confirmar que aparece la alerta accesible y no se dispara la recarga. | |
| Estado de reserva | Seleccionar “Confirmada” y validar que los totales se ajustan para reservas y KPIs. Quitar el filtro y confirmar valores globales. | |

## 3. Accesibilidad rápida

| Caso | Pasos | Resultado |
| --- | --- | --- |
| Navegación por teclado | Usar `Tab` desde la URL hasta el botón Actualizar, recorrer todos los filtros y botones. Asegurar que los focos visibles aparecen y no se pierde el foco en la página. | |
| Lectores de pantalla | Con NVDA o VoiceOver, navegar por los KPIs y un reporte. Verificar que las secciones anuncian sus títulos y que las tablas tienen descripciones. | |
| Alertas | Forzar una alerta (ej. sin token o rango inválido) y confirmar que el lector la anuncia inmediatamente. | |

## 4. Exportaciones y salida impresa

| Caso | Pasos | Resultado |
| --- | --- | --- |
| CSV | Presionar “Exportar CSV” y abrir el archivo generado. Confirmar presencia de todas las secciones y que el nombre incluye empresa + rango. | |
| Impresión | Presionar “Imprimir” y previsualizar. Verificar que botones/filtros se ocultan y que los gráficos/tablas se acomodan correctamente. | |

## 5. Respuestas del backend

| Caso | Pasos | Resultado |
| --- | --- | --- |
| Error 401/403 | Expirar sesión (eliminar token del storage) y refrescar. Confirmar alerta de error y redirección manual al login si corresponde. | |
| Error 500 | Simular indisponibilidad del backend (apagar servicio o configurar proxy). Confirmar alerta roja y que los botones se reactivan. | |
| Datos vacíos | Probar empresa o rango sin datos y validar mensajes “Sin datos disponibles” en tablas/gráficos. | |

## 6. Observaciones adicionales

- Registrar cualquier discrepancia de datos entre Reportes y los módulos fuente (reservas, ventas, etc.).
- Anotar tiempos de respuesta cuando el rango cubre más de 6 meses.
- Adjuntar capturas cuando sea necesario para auditoría.

> **Nota:** Si una prueba se automatiza posteriormente, añadir referencia al script o colección de Postman utilizada.
