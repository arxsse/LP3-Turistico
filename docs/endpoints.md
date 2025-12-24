# Endpoints

> Todos los endpoints requieren autenticación con token JWT en el encabezado `Authorization: Bearer <token>`.
> - Empresas: solo usuarios con rol `SUPERADMINISTRADOR`.
> - Roles, Usuarios y Sucursales: usuarios con rol `SUPERADMINISTRADOR` o `ADMINISTRADOR`.

## Autenticación

### Login - Obtener Token JWT

**POST** `/auth/login`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "admin@demoturistica.com",
  "password": "password"
}
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "usuario": {
      "idUsuario": 1,
      "nombre": "Admin",
      "apellido": "Sistema",
      "email": "admin@demoturistica.com",
      "rol": "SUPERADMINISTRADOR",
      "empresa": {
        "idEmpresa": 1,
        "nombreEmpresa": "Empresa Demo Turística"
      }
    },
    "expiracion": "2025-11-30T18:00:00Z"
  }
}
```

**Ejemplo PHP cURL:**
```php
<?php
$url = 'http://tu-servidor:8080/auth/login';
$data = json_encode([
    'email' => 'admin@demoturistica.com',
    'password' => 'password'
]);

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$result = json_decode($response, true);

if ($result['success']) {
    $token = $result['data']['token'];
    // Guardar token en sesión
    $_SESSION['token'] = $token;
}
?>
```

## Empresa

**POST** `/empresas`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreEmpresa": "Empresa Demo Turística S.A.",
  "ruc": "12345678901",
  "email": "admin@demoturistica.com",
  "telefono": "999888777",
  "direccion": "Av. Principal 123, Lima, Perú",
  "estado": 1
}
```

**Ejemplo PHP cURL:**
```php
<?php
$token = $_SESSION['token']; // Token JWT
$url = 'http://tu-servidor:8080/empresas';
$data = json_encode([
    'nombreEmpresa' => 'Empresa Demo Turística S.A.',
    'ruc' => '12345678901',
    'email' => 'admin@demoturistica.com',
    'telefono' => '999888777',
    'direccion' => 'Av. Principal 123, Lima, Perú',
    'estado' => 1
]);

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $token
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$result = json_decode($response, true);

if ($result['success']) {
    echo "Empresa creada exitosamente con ID: " . $result['data']['idEmpresa'];
} else {
    echo "Error: " . $result['message'];
}
?>
```

**GET** `/empresas`

**Headers:**
```
Authorization: Bearer <token>
```

**Parámetros de consulta (Query Parameters):**
- `busqueda` (opcional): texto para filtrar por nombre, email o RUC.
- `estado` (opcional): `1` activos, `0` inactivos.
- `page` (opcional): número de página (default: 1).
- `size` (opcional): elementos por página (default: 10).

**Ejemplo completo:**
```
GET /empresas?busqueda=Demo&estado=1&page=1&size=10
Authorization: Bearer <token>
```

**GET** `/empresas/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

**PUT** `/empresas/{id}`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreEmpresa": "Empresa Demo Turística Actualizada S.A.",
  "ruc": "12345678901",
  "email": "admin@demoturistica.com",
  "telefono": "999888888",
  "direccion": "Av. Actualizada 456, Lima, Perú",
  "estado": 1
}
```

**DELETE** `/empresas/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

## Roles

**POST** `/roles`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreRol": "Administrador",
  "descripcion": "Rol con acceso administrativo completo al sistema",
  "estado": 1
}
```

**GET** `/roles`

**Headers:**
```
Authorization: Bearer <token>
```

**Parámetros de consulta:**
- `busqueda` (opcional): texto para filtrar por nombre o descripción.
- `estado` (opcional): `1` activos, `0` inactivos.
- `page` (opcional): número de página (default: 1).
- `size` (opcional): elementos por página (default: 10).

**Ejemplo completo:**
```
GET /roles?busqueda=Admin&estado=1&page=1&size=10
Authorization: Bearer <token>
```

**GET** `/roles/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

**PUT** `/roles/{id}`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreRol": "Administrador",
  "descripcion": "Rol con acceso administrativo completo actualizado",
  "estado": 1
}
```

**DELETE** `/roles/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

## Usuarios

**POST** `/usuarios`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombre": "Juan Carlos",
  "apellido": "Pérez Ramírez",
  "email": "juan.perez@empresa.com",
  "password": "Secreta123!",
  "dni": "12345678",
  "telefono": "999888777",
  "fechaNacimiento": "1990-05-15",
  "estado": 1,
  "empresaId": 1,
  "rolId": 2,
  "sucursalId": 1
}
```

**GET** `/usuarios`

**Headers:**
```
Authorization: Bearer <token>
```

**Parámetros de consulta:**
- `busqueda` (opcional): filtra por nombre, apellido, email o DNI.
- `estado` (opcional): `1` activos, `0` inactivos.
- `empresaId` (opcional): filtra por empresa.
- `rolId` (opcional): filtra por rol.
- `sucursalId` (opcional): filtra por sucursal.
- `page` (opcional): número de página (default: 1).
- `size` (opcional): elementos por página (default: 10).

**Ejemplo completo:**
```
GET /usuarios?busqueda=Juan&estado=1&empresaId=1&page=1&size=10
Authorization: Bearer <token>
```

**GET** `/usuarios/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

**PUT** `/usuarios/{id}`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombre": "Juan Carlos",
  "apellido": "Pérez Ramírez Actualizado",
  "email": "juan.perez@empresa.com",
  "password": "NuevaClave123!",
  "dni": "12345678",
  "telefono": "999888888",
  "fechaNacimiento": "1990-05-15",
  "estado": 1,
  "empresaId": 1,
  "rolId": 2,
  "sucursalId": 1
}
```

**DELETE** `/usuarios/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

## Sucursales

**POST** `/sucursales`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreSucursal": "Sucursal Miraflores",
  "ubicacion": "Miraflores, Lima, Perú",
  "direccion": "Av. Larco 123, Miraflores, Lima",
  "telefono": "999888777",
  "email": "miraflores@empresa.com",
  "gerente": "Luis Pérez Ramírez",
  "estado": 1,
  "empresaId": 1
}
```

**GET** `/sucursales`

**Headers:**
```
Authorization: Bearer <token>
```

**Parámetros de consulta:**
- `busqueda` (opcional): filtra por nombre, ubicación, email, dirección o gerente.
- `estado` (opcional): `1` activos, `0` inactivos.
- `empresaId` (opcional): filtra por empresa.
- `page` (opcional): número de página (default: 1).
- `size` (opcional): elementos por página (default: 10).

**Ejemplo completo:**
```
GET /sucursales?busqueda=Miraflores&estado=1&empresaId=1&page=1&size=10
Authorization: Bearer <token>
```

**GET** `/sucursales/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

**PUT** `/sucursales/{id}`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "nombreSucursal": "Sucursal Miraflores Centro",
  "ubicacion": "Miraflores, Lima, Perú",
  "direccion": "Av. Larco 456, Miraflores, Lima",
  "telefono": "999888888",
  "email": "miraflores@empresa.com",
  "gerente": "Ana Gómez Martínez",
  "estado": 1,
  "empresaId": 1
}
```

**DELETE** `/sucursales/{id}`

**Headers:**
```
Authorization: Bearer <token>
```

## Reportes

Los reportes están diseñados para ser multi-empresa. Los administradores solo pueden ver datos de su propia empresa, mientras que los superadministradores pueden especificar `empresaId` para ver datos de otras empresas.

### Reporte de Reservas

**GET** `/reportes/reservas`

**Parámetros de consulta (Query Parameters):**
- `empresaId` (opcional): ID de la empresa para filtrar. Solo para superadministradores.
- `fechaInicio` (opcional): Fecha de inicio en formato YYYY-MM-DD.
- `fechaFin` (opcional): Fecha de fin en formato YYYY-MM-DD.
- `estado` (opcional): Estado de la reserva (Pendiente, Confirmada, PagoParcial, Pagada, Cancelada, Completada).

**Ejemplo de solicitud en Postman:**
```
GET /reportes/reservas?fechaInicio=2025-11-01&fechaFin=2025-11-30&estado=Confirmada
Authorization: Bearer <token>
```

**Ejemplo PHP cURL:**
```php
<?php
$token = $_SESSION['token']; // Token obtenido del login
$url = 'http://tu-servidor:8080/reportes/reservas?fechaInicio=2025-11-01&fechaFin=2025-11-30&estado=Confirmada';

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $token
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$result = json_decode($response, true);

if ($result['success']) {
    echo "Total reservas: " . $result['data']['totalReservas'];
    echo "Monto total: S/ " . $result['data']['totalMonto'];
}
?>

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de reservas obtenido correctamente",
  "data": {
    "fechaInicio": "2025-11-01",
    "fechaFin": "2025-11-30",
    "totalReservas": 25,
    "reservasPorEstado": {
      "Confirmada": 15,
      "Pagada": 8,
      "Cancelada": 2
    },
    "totalMonto": 12500.00
  }
}
```

### Reporte de Ventas

**GET** `/reportes/ventas`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.
- `fechaInicio` (opcional): Fecha de inicio en formato YYYY-MM-DD.
- `fechaFin` (opcional): Fecha de fin en formato YYYY-MM-DD.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/ventas?fechaInicio=2025-11-01&fechaFin=2025-11-30
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de ventas obtenido correctamente",
  "data": {
    "fechaInicio": "2025-11-01",
    "fechaFin": "2025-11-30",
    "totalVentas": 45,
    "montoTotal": 28500.50,
    "ventasPorMetodoPago": {
      "Efectivo": 25,
      "Transferencia": 15,
      "Tarjeta": 5
    }
  }
}
```

### Reporte de Clientes

**GET** `/reportes/clientes`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/clientes
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de clientes obtenido correctamente",
  "data": {
    "totalClientes": 150,
    "clientesActivos": 142,
    "clientesPorNacionalidad": {
      "Peruano": 120,
      "Chileno": 15,
      "Colombiano": 7
    },
    "clientesPorNivelMembresia": {
      "Bronce": 80,
      "Plata": 45,
      "Oro": 22,
      "Platino": 3
    }
  }
}
```

### Reporte de Personal

**GET** `/reportes/personal`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/personal
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de personal obtenido correctamente",
  "data": {
    "totalPersonal": 12,
    "personalActivo": 11,
    "personalPorCargo": {
      "Guía": 6,
      "Chofer": 4,
      "Staff": 2
    },
    "personalPorTurno": {
      "Completo": 8,
      "Mañana": 2,
      "Tarde": 1
    }
  }
}
```

### Reporte de Servicios

**GET** `/reportes/servicios`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/servicios
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de servicios obtenido correctamente",
  "data": {
    "totalServicios": 8,
    "serviciosActivos": 7,
    "serviciosPorTipo": {
      "Tour": 5,
      "Hotel": 2,
      "Transporte": 1
    },
    "serviciosPorCategoria": {
      "1": 3,
      "2": 2,
      "4": 2
    }
  }
}
```

### Reporte de Paquetes

**GET** `/reportes/paquetes`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/paquetes
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de paquetes obtenido correctamente",
  "data": {
    "totalPaquetes": 3,
    "paquetesActivos": 3,
    "paquetesConPromocion": 1,
    "precioPromedio": 450.00
  }
}
```

### Reporte de Evaluaciones

**GET** `/reportes/evaluaciones`

**Parámetros de consulta:**
- `empresaId` (opcional): ID de la empresa para filtrar.
- `fechaInicio` (opcional): Fecha de inicio en formato YYYY-MM-DD.
- `fechaFin` (opcional): Fecha de fin en formato YYYY-MM-DD.

**Ejemplo de solicitud en Postman:**
```
GET /reportes/evaluaciones?fechaInicio=2025-11-01&fechaFin=2025-11-30
Authorization: Bearer <token>
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "message": "Reporte de evaluaciones obtenido correctamente",
  "data": {
    "fechaInicio": "2025-11-01",
    "fechaFin": "2025-11-30",
    "totalEvaluaciones": 23,
    "promedioGeneral": 4.2,
    "promedioGuia": 4.1
  }
}
```
