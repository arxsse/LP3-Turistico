# Guía Postman reservas (login, logout y pruebas)

> Configura en Postman la variable `baseUrl` con `http://localhost:8080/api/v1` (ajusta si usas otro puerto). También crea la variable `token` para guardar el JWT después del login.

## 1. Autenticación

### Login (usuario Kael)
- POST {{baseUrl}}/auth/login
- Headers: Content-Type: application/json
- Body:
  {
    "email": "kaeld@gmail.com",
    "password": "<clave_kael>"
  }
- Tests (opcional):
  const res = pm.response.json();
  if (res.token) {
    pm.environment.set('token', res.token);
  }

### Logout
- POST {{baseUrl}}/auth/logout
- Headers: Content-Type: application/json, Authorization: Bearer {{token}}
- Body: (vacío)

## 2. Reservas (endpoints principales)
Incluye siempre los headers Content-Type: application/json y Authorization: Bearer {{token}}.

### Crear reserva (SOLO SERVICIO)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 3,
    "fechaServicio": "2025-12-22",
    "observaciones": "Reserva solo servicio",
    "items": [
      {
        "tipoItem": "SERVICIO",
        "servicioId": 3,
        "cantidad": 3,
        "precioUnitario": 320.00,
        "precioTotal": 960.00,
        "descripcionExtra": "Tour privado para 3 personas"
      }
    ]
  }

### Crear reserva (SOLO PAQUETE)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 2,
    "fechaServicio": "2025-12-25",
    "observaciones": "Reserva solo paquete",
    "items": [
      {
        "tipoItem": "PAQUETE",
        "paqueteId": 2,
        "cantidad": 2,
        "precioUnitario": 520.00,
        "precioTotal": 1040.00,
        "descripcionExtra": "Paquete Escápate a la Selva"
      }
    ]
  }

### Crear reserva (SERVICIO + PAQUETE)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 2,
    "fechaServicio": "2025-12-20",
    "observaciones": "Reserva QA mezcla servicio y paquete",
    "items": [
      {
        "tipoItem": "SERVICIO",
        "servicioId": 3,
        "cantidad": 2,
        "precioUnitario": 320.00,
        "precioTotal": 640.00,
        "descripcionExtra": "Tour compartido"
      },
      {
        "tipoItem": "PAQUETE",
        "paqueteId": 2,
        "cantidad": 1,
        "precioUnitario": 520.00,
        "precioTotal": 520.00,
        "descripcionExtra": "Paquete Escápate"
      }
    ]
  }

### Crear reserva (MÚLTIPLES SERVICIOS)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 4,
    "fechaServicio": "2025-12-28",
    "observaciones": "Reserva con varios servicios",
    "items": [
      {
        "tipoItem": "SERVICIO",
        "servicioId": 3,
        "cantidad": 4,
        "precioUnitario": 320.00,
        "precioTotal": 1280.00,
        "descripcionExtra": "City Tour para 4 personas"
      },
      {
        "tipoItem": "SERVICIO",
        "servicioId": 5,
        "cantidad": 4,
        "precioUnitario": 150.00,
        "precioTotal": 600.00,
        "descripcionExtra": "Transporte adicional"
      },
      {
        "tipoItem": "SERVICIO",
        "servicioId": 7,
        "cantidad": 4,
        "precioUnitario": 80.00,
        "precioTotal": 320.00,
        "descripcionExtra": "Almuerzo típico"
      }
    ]
  }

### Crear reserva (MÚLTIPLES PAQUETES)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 3,
    "fechaServicio": "2025-12-30",
    "observaciones": "Reserva con varios paquetes",
    "items": [
      {
        "tipoItem": "PAQUETE",
        "paqueteId": 2,
        "cantidad": 2,
        "precioUnitario": 520.00,
        "precioTotal": 1040.00,
        "descripcionExtra": "Paquete Escápate - Pareja"
      },
      {
        "tipoItem": "PAQUETE",
        "paqueteId": 1,
        "cantidad": 1,
        "precioUnitario": 500.00,
        "precioTotal": 500.00,
        "descripcionExtra": "Paquete Aventura - Individual"
      }
    ]
  }

### Crear reserva (COMBINACIÓN COMPLETA)
- POST {{baseUrl}}/reservas
- Body:
  {
    "empresaId": 6,
    "clienteId": 15,
    "numeroPersonas": 5,
    "fechaServicio": "2026-01-05",
    "descuentoAplicado": 100.00,
    "observaciones": "Reserva familiar completa con descuento",
    "items": [
      {
        "tipoItem": "SERVICIO",
        "servicioId": 3,
        "cantidad": 3,
        "precioUnitario": 320.00,
        "precioTotal": 960.00,
        "descripcionExtra": "City Tour - 3 adultos"
      },
      {
        "tipoItem": "SERVICIO",
        "servicioId": 5,
        "cantidad": 5,
        "precioUnitario": 150.00,
        "precioTotal": 750.00,
        "descripcionExtra": "Transporte para todos"
      },
      {
        "tipoItem": "PAQUETE",
        "paqueteId": 2,
        "cantidad": 1,
        "precioUnitario": 520.00,
        "precioTotal": 520.00,
        "descripcionExtra": "Paquete Escápate"
      }
    ]
  }

## Notas importantes sobre items:

1. **Array de items**: El campo `items` es un ARRAY, puedes enviar múltiples items en una sola reserva
2. **Tipos válidos**: `tipoItem` puede ser "SERVICIO" o "PAQUETE"
3. **IDs requeridos**: 
   - Si `tipoItem` = "SERVICIO", debes enviar `servicioId`
   - Si `tipoItem` = "PAQUETE", debes enviar `paqueteId`
4. **Cálculo automático**: El `precioTotal` se calcula como `cantidad * precioUnitario`
5. **Descripción opcional**: `descripcionExtra` es opcional, úsalo para notas específicas del item

### Listar reservas por empresa
- GET {{baseUrl}}/reservas?empresaId=6

### Obtener reserva por ID
- GET {{baseUrl}}/reservas/{{idReserva}}

### Actualizar estado/observaciones
- PUT {{baseUrl}}/reservas/{{idReserva}}
- Body:
  {
    "estado": "Confirmada",
    "observaciones": "Actualizada desde Postman"
  }

### Cancelar, completar y evaluar
- PUT {{baseUrl}}/reservas/{{idReserva}}/cancelar
- PUT {{baseUrl}}/reservas/{{idReserva}}/completar
- PUT {{baseUrl}}/reservas/{{idReserva}}/evaluar

### Eliminar (soft delete)
- DELETE {{baseUrl}}/reservas/{{idReserva}}

### Reservas próximas (7 días)
- GET {{baseUrl}}/reservas/proximas?empresaId=6

## 3. Exportar código PHP cURL desde Postman
1. Ejecuta la petición que quieras exportar.
2. Haz clic en el botón </> (Code).
3. Selecciona "PHP - cURL".
4. Copia el snippet y pégalo en la vista PHP correspondiente.
