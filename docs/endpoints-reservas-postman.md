# Endpoints de Reservas para Postman

## Configuración Base
- **Base URL**: `http://turistas.spring.informaticapp.com:2410/api/v1`
- **Context Path**: `/api/v1`
- **Headers requeridos** (excepto login):
  - `Content-Type: application/json`
  - `Authorization: Bearer {token}` (obtenido del endpoint de login)

---

## 1. Crear Reserva
**POST** `/reservas`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Body (JSON):**
```json
{
  "empresaId": 1,
  "idSucursal": 1,
  "clienteId": 1,
  "promocionId": null,
  "codigoReserva": "RES-2024-001",
  "fechaServicio": "2024-12-25T10:00:00",
  "fechaReserva": "2024-12-18T09:00:00",
  "numeroPersonas": 4,
  "descuentoAplicado": 50.00,
  "observaciones": "Cliente requiere transporte desde el hotel",
  "items": [
    {
      "tipoItem": "SERVICIO",
      "servicioId": 1,
      "paqueteId": null,
      "cantidad": 2,
      "precioUnitario": 150.00,
      "precioTotal": 300.00,
      "descripcionExtra": "Tour guiado en español"
    },
    {
      "tipoItem": "PAQUETE",
      "servicioId": null,
      "paqueteId": 1,
      "cantidad": 1,
      "precioUnitario": 500.00,
      "precioTotal": 500.00,
      "descripcionExtra": null
    }
  ],
  "asignaciones": [
    {
      "idPersonal": 1,
      "fechaAsignacion": "2024-12-25",
      "observaciones": "Guía principal"
    },
    {
      "idPersonal": 2,
      "fechaAsignacion": "2024-12-25",
      "observaciones": "Asistente"
    }
  ]
}
```

**Campos requeridos:**
- `clienteId` (obligatorio)
- `fechaServicio` (obligatorio, formato ISO 8601)
- `items` (obligatorio, mínimo 1 item)

**Campos opcionales:**
- `empresaId` - Si no se proporciona, se usa la empresa del usuario autenticado
- `idSucursal` - ID de la sucursal (opcional)
- `codigoReserva` - Si no se proporciona, se genera automáticamente
- `fechaReserva` - Si no se proporciona, se usa la fecha actual
- `numeroPersonas` - Se calcula automáticamente desde los items si no se proporciona
- `descuentoAplicado` - Por defecto 0.00
- `observaciones` - Opcional
- `promocionId` - Opcional
- `asignaciones` - Opcional, máximo 50

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Notas:**
- `tipoItem` puede ser: `"SERVICIO"` o `"PAQUETE"`
- Si es `SERVICIO`, debe proporcionar `servicioId`
- Si es `PAQUETE`, debe proporcionar `paqueteId`
- `items` debe tener al menos un elemento
- El precio total se calcula automáticamente si no se proporciona

---

## 2. Listar Reservas
**GET** `/reservas`

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `busqueda` (opcional): String - Búsqueda por código de reserva, nombre o apellido del cliente
- `estado` (opcional): String - Estados: `Pendiente`, `Confirmada`, `PagoParcial`, `Pagada`, `Cancelada`, `Completada`
- `page` (opcional): Integer - Número de página (default: 1)
- `size` (opcional): Integer - Tamaño de página (default: 10)
- `empresaId` (opcional): Long - ID de empresa

**Ejemplos:**
```
GET /reservas?estado=Pendiente&page=1&size=10
GET /reservas?busqueda=RES-2024&page=1&size=20
GET /reservas?empresaId=1&estado=Pagada
GET /reservas
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Respuesta:**
```json
{
  "success": true,
  "message": "Reservas obtenidas exitosamente",
  "data": [
    {
      "idReserva": 1,
      "codigoReserva": "RES-2024-001",
      "fechaReserva": "2024-12-18",
      "fechaServicio": "2024-12-25",
      "numeroPersonas": 4,
      "precioTotal": 750.00,
      "descuentoAplicado": 50.00,
      "estado": "Pendiente",
      "observaciones": "Cliente requiere transporte",
      "evaluada": false,
      "idSucursal": 1,
      "nombreSucursal": "Sucursal Centro",
      "idCliente": 1,
      "nombreCliente": "Juan",
      "apellidoCliente": "Pérez",
      "emailCliente": "juan@example.com",
      "telefonoCliente": "999888777",
      "idServicio": 1,
      "nombreServicio": "Tour Histórico",
      "tipoServicio": "Tour",
      "idPaquete": null,
      "nombrePaquete": null,
      "idUsuario": 1,
      "nombreUsuario": "Admin",
      "apellidoUsuario": "Sistema",
      "items": [...],
      "asignaciones": [...]
    }
  ],
  "total": 1,
  "page": 1,
  "size": 10,
  "totalPages": 1
}
```

---

## 3. Obtener Reserva por ID
**GET** `/reservas/{id}`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
GET /reservas/111
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Respuesta:**
```json
{
  "success": true,
  "message": "Reserva obtenida exitosamente",
  "data": {
    "idReserva": 111,
    "codigoReserva": "RES-2024-001",
    "fechaReserva": "2024-12-18",
    "fechaServicio": "2024-12-25",
    "numeroPersonas": 4,
    "precioTotal": 750.00,
    "descuentoAplicado": 50.00,
    "estado": "Pendiente",
    "observaciones": "Cliente requiere transporte",
    "evaluada": false,
    "createdAt": "2024-12-18T09:00:00",
    "updatedAt": "2024-12-18T09:00:00",
    "idSucursal": 1,
    "nombreSucursal": "Sucursal Centro",
    "idCliente": 1,
    "nombreCliente": "Juan",
    "apellidoCliente": "Pérez",
    "emailCliente": "juan@example.com",
    "telefonoCliente": "999888777",
    "idServicio": 1,
    "nombreServicio": "Tour Histórico",
    "tipoServicio": "Tour",
    "idPaquete": null,
    "nombrePaquete": null,
    "idUsuario": 1,
    "nombreUsuario": "Admin",
    "apellidoUsuario": "Sistema",
    "items": [
      {
        "idReservaItem": 1,
        "tipoItem": "SERVICIO",
        "idServicio": 1,
        "nombreServicio": "Tour Histórico",
        "tipoServicio": "Tour",
        "idPaquete": null,
        "nombrePaquete": null,
        "cantidad": 2,
        "precioUnitario": 150.00,
        "precioTotal": 300.00,
        "descripcionExtra": "Tour guiado en español"
      }
    ],
    "asignaciones": [
      {
        "idAsignacion": 1,
        "idPersonal": 1,
        "nombrePersonal": "Carlos",
        "apellidoPersonal": "Mendoza",
        "dniPersonal": "75146930",
        "telefonoPersonal": "999111222",
        "emailPersonal": "carlos@example.com",
        "cargoPersonal": "Guía",
        "rolAsignado": "Guía",
        "estado": "Asignado",
        "observaciones": "Guía principal",
        "fechaAsignacion": "2024-12-25",
        "createdAt": "2024-12-18T09:00:00",
        "updatedAt": "2024-12-18T09:00:00"
      }
    ]
  }
}
```

---

## 4. Actualizar Reserva (Actualización Básica)
**PUT** `/reservas/{id}`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Body (JSON):**
```json
{
  "estado": "Confirmada",
  "observaciones": "Reserva confirmada, cliente informado"
}
```

**Campos modificables:**
- `estado` (opcional): Cambiar el estado de la reserva
- `observaciones` (opcional): Actualizar las observaciones

**Restricciones:**
- Solo funciona para reservas **activas** (no canceladas ni completadas)
- Debe proporcionar al menos un campo para actualizar
- El cambio de estado debe seguir el flujo válido de estados
- No se pueden modificar: items, asignaciones, fechas, precios, número de personas

**Estados posibles:**
- `Pendiente`
- `Confirmada`
- `PagoParcial`
- `Pagada`
- `Cancelada`
- `Completada`

**Ejemplo:**
```
PUT /reservas/111
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Nota:** Este endpoint es para cambios simples como actualizar el estado o las observaciones. Para cambios más profundos, usa el endpoint `/reservas/{id}/detalle`

---

## 5. Actualizar Detalle de Reserva (Edición Completa)
**PUT** `/reservas/{id}/detalle`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Body (JSON):**
```json
{
  "empresaId": 1,
  "fechaServicio": "2024-12-26T14:00:00",
  "numeroPersonas": 6,
  "descuentoAplicado": 75.50,
  "observaciones": "Cambio de fecha por solicitud del cliente",
  "items": [
    {
      "idReservaItem": 1,
      "tipoItem": "SERVICIO",
      "servicioId": 1,
      "paqueteId": null,
      "cantidad": 3,
      "precioUnitario": 150.00,
      "precioTotal": 450.00,
      "descripcionExtra": "Actualizado"
    }
  ],
  "asignaciones": [
    {
      "idAsignacion": 1,
      "idPersonal": 1,
      "fechaAsignacion": "2024-12-26",
      "observaciones": "Guía principal actualizado"
    }
  ],
  "sincronizarAsignaciones": true
}
```

**Campos modificables:**
- `fechaServicio` (obligatorio): Cambiar la fecha del servicio
- `numeroPersonas` (opcional): Cambiar el número de personas (si no se proporciona, se calcula desde los items)
- `descuentoAplicado` (opcional): Cambiar el descuento aplicado
- `observaciones` (opcional): Actualizar las observaciones
- `items` (obligatorio): Modificar, agregar o eliminar items de la reserva
  - Cada item puede tener: `idReservaItem` (para actualizar existente), `tipoItem`, `servicioId` o `paqueteId`, `cantidad`, `precioUnitario`, `precioTotal`, `descripcionExtra`
- `asignaciones` (opcional): Modificar el personal asignado
  - Cada asignación puede tener: `idAsignacion` (para actualizar existente), `idPersonal`, `fechaAsignacion`, `observaciones`
- `sincronizarAsignaciones` (opcional): Boolean
  - `true`: Reemplaza completamente las asignaciones (lista vacía elimina todas)
  - `false` o `null`: Solo actualiza las asignaciones proporcionadas

**Restricciones importantes:**
- ⚠️ **Solo válido para reservas en estado `Pendiente`**
- `fechaServicio` es obligatorio (formato ISO 8601: `yyyy-MM-ddTHH:mm:ss`)
- `items` es obligatorio, debe tener al menos 1 item
- El precio total se recalcula automáticamente desde los items
- El número de personas se recalcula automáticamente si no se proporciona
- Máximo 50 asignaciones

**Campos que NO se pueden modificar:**
- `codigoReserva` - No se puede cambiar
- `fechaReserva` - No se puede cambiar
- `clienteId` - No se puede cambiar el cliente
- `idSucursal` - No se puede cambiar la sucursal (en este endpoint)
- `estado` - No se puede cambiar el estado (usa el endpoint `/reservas/{id}`)

**Ejemplo:**
```
PUT /reservas/111/detalle
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Nota:** Este endpoint permite una edición completa de los detalles de la reserva, pero solo para reservas pendientes. Para cambiar el estado, usa el endpoint `/reservas/{id}`

---

## 6. Obtener Asignaciones de Reserva
**GET** `/reservas/{id}/asignaciones`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
GET /reservas/111/asignaciones
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Respuesta:**
```json
{
  "success": true,
  "message": "Asignaciones obtenidas exitosamente",
  "data": [
    {
      "idAsignacion": 1,
      "idPersonal": 1,
      "nombrePersonal": "Carlos",
      "apellidoPersonal": "Mendoza",
      "dniPersonal": "75146930",
      "telefonoPersonal": "999111222",
      "emailPersonal": "carlos@example.com",
      "cargoPersonal": "Guía",
      "rolAsignado": "Guía",
      "estado": "Asignado",
      "observaciones": "Guía principal",
      "fechaAsignacion": "2024-12-25",
      "createdAt": "2024-12-18T09:00:00",
      "updatedAt": "2024-12-18T09:00:00"
    }
  ],
  "total": 1
}
```

---

## 7. Sincronizar Asignaciones de Reserva
**PUT** `/reservas/{id}/asignaciones`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Body (JSON):**
```json
{
  "asignaciones": [
    {
      "idPersonal": 1,
      "fechaAsignacion": "2024-12-25",
      "observaciones": "Guía principal"
    },
    {
      "idPersonal": 3,
      "fechaAsignacion": "2024-12-25",
      "observaciones": "Nuevo asistente"
    }
  ]
}
```

**Notas:**
- Solo válido para reservas en estado `Pendiente` o `Pagada`
- Reemplaza completamente las asignaciones existentes
- Lista vacía eliminará todas las asignaciones
- Máximo 50 asignaciones

**Ejemplo:**
```
PUT /reservas/111/asignaciones
```

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 8. Eliminar Reserva
**DELETE** `/reservas/{id}`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
DELETE /reservas/111
```

**Nota:** Realiza un borrado lógico (soft delete)

**Roles permitidos:** ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 9. Cancelar Reserva
**PUT** `/reservas/{id}/cancelar`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
PUT /reservas/111/cancelar
```

**Nota:** Cambia el estado de la reserva a `Cancelada`

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 10. Completar Reserva
**PUT** `/reservas/{id}/completar`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
PUT /reservas/111/completar
```

**Nota:** Cambia el estado de la reserva a `Completada` después de finalizar el servicio

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 11. Marcar Reserva como Evaluada
**PUT** `/reservas/{id}/evaluar`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
PUT /reservas/111/evaluar
```

**Nota:** Marca una reserva completada como evaluada. Solo válido para reservas en estado `Completada` que no hayan sido evaluadas.

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 12. Obtener Reservas Próximas
**GET** `/reservas/proximas`

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `empresaId` (opcional): Long - ID de empresa

**Ejemplo:**
```
GET /reservas/proximas
GET /reservas/proximas?empresaId=1
```

**Nota:** Retorna las reservas de los próximos 7 días en estado `Confirmada` o `Pagada`

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

---

## 13. Obtener Voucher de Reserva
**GET** `/reservas/{id}/voucher`

**Headers:**
```
Authorization: Bearer {token}
```

**Ejemplo:**
```
GET /reservas/111/voucher
```

**Nota:** Obtiene el voucher asociado a una reserva específica. Retorna 404 si la reserva no tiene voucher.

**Roles permitidos:** EMPLEADO, ADMINISTRADOR, SUPERADMINISTRADOR, GERENTE

**Respuesta:**
```json
{
  "success": true,
  "message": "Voucher obtenido exitosamente",
  "data": {
    "idVoucher": 1,
    "codigoVoucher": "VOU-2024-001",
    "idReserva": 111,
    "codigoReserva": "RES-2024-001",
    "fechaEmision": "2024-12-18T09:00:00",
    "fechaVencimiento": "2024-12-25T10:00:00",
    "estado": "Activo",
    "createdAt": "2024-12-18T09:00:00"
  }
}
```

---

## Estados de Reserva

Los estados posibles son:
- `Pendiente`: Reserva creada, esperando confirmación
- `Confirmada`: Reserva confirmada
- `PagoParcial`: Reserva con pago parcial
- `Pagada`: Reserva completamente pagada
- `Cancelada`: Reserva cancelada
- `Completada`: Servicio completado

**Flujo de estados:**
```
Pendiente → Confirmada → Pagada → Completada → Evaluada
         ↓
      Cancelada
```

---

## Tipos de Item

- `SERVICIO`: Item de tipo servicio individual
- `PAQUETE`: Item de tipo paquete turístico

---

## Estructura de ReservaResponse

La respuesta incluye los siguientes campos:

```json
{
  "idReserva": 1,
  "codigoReserva": "RES-2024-001",
  "fechaReserva": "2024-12-18",
  "fechaServicio": "2024-12-25",
  "numeroPersonas": 4,
  "precioTotal": 750.00,
  "descuentoAplicado": 50.00,
  "estado": "Pendiente",
  "observaciones": "Observaciones de la reserva",
  "evaluada": false,
  "createdAt": "2024-12-18T09:00:00",
  "updatedAt": "2024-12-18T09:00:00",
  "idSucursal": 1,
  "nombreSucursal": "Sucursal Centro",
  "idCliente": 1,
  "nombreCliente": "Juan",
  "apellidoCliente": "Pérez",
  "emailCliente": "juan@example.com",
  "telefonoCliente": "999888777",
  "idServicio": 1,
  "nombreServicio": "Tour Histórico",
  "tipoServicio": "Tour",
  "idPaquete": null,
  "nombrePaquete": null,
  "idUsuario": 1,
  "nombreUsuario": "Admin",
  "apellidoUsuario": "Sistema",
  "items": [...],
  "asignaciones": [...]
}
```

---

## Códigos de Estado HTTP

- `200 OK`: Operación exitosa
- `400 Bad Request`: Error de validación o datos inválidos
- `401 Unauthorized`: Token inválido o ausente
- `403 Forbidden`: No tiene permisos para esta operación
- `404 Not Found`: Recurso no encontrado
- `409 Conflict`: Conflicto (ej: intentar editar reserva no pendiente)
- `500 Internal Server Error`: Error interno del servidor

---

## Ejemplo de Respuesta Exitosa

```json
{
  "success": true,
  "message": "Reserva creada exitosamente",
  "data": {
    "idReserva": 1,
    "codigoReserva": "RES-2024-001",
    "estado": "Pendiente",
    "fechaServicio": "2024-12-25",
    "fechaReserva": "2024-12-18",
    "numeroPersonas": 4,
    "descuentoAplicado": 50.00,
    "observaciones": "Cliente requiere transporte desde el hotel",
    "precioTotal": 750.00,
    "idSucursal": 1,
    "nombreSucursal": "Sucursal Centro",
    "idCliente": 1,
    "nombreCliente": "Juan",
    "apellidoCliente": "Pérez",
    "items": [...],
    "asignaciones": [...]
  }
}
```

---

## Ejemplo de Respuesta de Error

```json
{
  "success": false,
  "message": "El cliente es obligatorio"
}
```

---

## Importar Colección de Postman

Para facilitar las pruebas, puedes crear una colección en Postman con estas variables de entorno:

**Variables:**
- `base_url`: `http://turistas.spring.informaticapp.com:2410/api/v1`
- `token`: `{token obtenido del login}`

**Ejemplo de uso en Postman:**
- URL: `{{base_url}}/reservas`
- Header: `Authorization: Bearer {{token}}`

**Pre-request Script para obtener token automáticamente:**
```javascript
// Si necesitas obtener el token automáticamente
pm.sendRequest({
    url: pm.environment.get("base_url") + "/auth/login",
    method: 'POST',
    header: {
        'Content-Type': 'application/json'
    },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            email: 'tu-email@example.com',
            password: 'tu-password'
        })
    }
}, function (err, res) {
    if (err) {
        console.log(err);
    } else {
        var jsonData = res.json();
        pm.environment.set("token", jsonData.token || jsonData.accessToken);
    }
});
```

---

## Resumen: ¿Qué Puedo Cambiar al Editar una Reserva?

### Endpoint: PUT `/reservas/{id}` (Actualización Básica)
**Para reservas activas** (Pendiente, Confirmada, PagoParcial, Pagada)

✅ **Puedes cambiar:**
- `estado` - Cambiar el estado de la reserva
- `observaciones` - Actualizar las observaciones

❌ **NO puedes cambiar:**
- Items (servicios/paquetes)
- Asignaciones de personal
- Fecha de servicio
- Fecha de reserva
- Número de personas
- Descuento aplicado
- Precio total
- Cliente
- Sucursal

---

### Endpoint: PUT `/reservas/{id}/detalle` (Edición Completa)
**Solo para reservas en estado `Pendiente`**

✅ **Puedes cambiar:**
- `fechaServicio` - Cambiar la fecha del servicio ⚠️ (obligatorio)
- `numeroPersonas` - Cambiar el número de personas
- `descuentoAplicado` - Cambiar el descuento
- `observaciones` - Actualizar las observaciones
- `items` - Modificar, agregar o eliminar items ⚠️ (obligatorio, mínimo 1)
  - Puedes cambiar: tipo, servicio/paquete, cantidad, precios, descripción
- `asignaciones` - Modificar el personal asignado
  - Puedes agregar, actualizar o eliminar asignaciones
  - Usa `sincronizarAsignaciones: true` para reemplazar todas

❌ **NO puedes cambiar:**
- `codigoReserva` - No se puede cambiar
- `fechaReserva` - No se puede cambiar
- `clienteId` - No se puede cambiar el cliente
- `idSucursal` - No se puede cambiar la sucursal
- `estado` - No se puede cambiar el estado (usa el otro endpoint)

---

## Notas Importantes

1. **Autenticación**: Todos los endpoints requieren un token JWT válido en el header `Authorization: Bearer {token}`

2. **Multi-tenant**: El sistema es multi-tenant. Si no se proporciona `empresaId`, se usa la empresa del usuario autenticado.

3. **Sucursales**: El campo `idSucursal` es opcional al crear. Una vez creada, no se puede cambiar.

4. **Código de Reserva**: Si no se proporciona `codigoReserva` al crear, se genera automáticamente con el formato: `RES-{empresaId}-{fecha}-{secuencial}`. Una vez creado, no se puede cambiar.

5. **Precio Total**: Se calcula automáticamente sumando los `precioTotal` de todos los items, menos el `descuentoAplicado`.

6. **Número de Personas**: Si no se proporciona, se calcula automáticamente sumando las `cantidad` de todos los items.

7. **Edición de Reservas**: 
   - **Cambios básicos** (estado, observaciones): Usa `PUT /reservas/{id}` - Funciona para reservas activas
   - **Cambios completos** (items, fechas, asignaciones): Usa `PUT /reservas/{id}/detalle` - Solo para reservas en estado `Pendiente`

8. **Asignaciones**: 
   - Se pueden modificar en reservas con estado `Pendiente` usando `/reservas/{id}/detalle`
   - Se pueden sincronizar en reservas con estado `Pendiente` o `Pagada` usando `/reservas/{id}/asignaciones`

9. **Flujo recomendado:**
   - Crear reserva → Estado: `Pendiente`
   - Editar detalles (items, fechas, asignaciones) → Solo si está `Pendiente`
   - Cambiar estado → Usar `PUT /reservas/{id}` con nuevo estado
   - Una vez que pasa a `Confirmada`, `Pagada`, etc. → Solo se pueden cambiar estado y observaciones
