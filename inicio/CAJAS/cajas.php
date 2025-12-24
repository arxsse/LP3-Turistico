<?php
// Serve CAJAS to the admin panel using AJAX (igual que reservas)
$isAjax = isset($_GET['ajax']) && $_GET['ajax'] === '1';

if (!$isAjax) {
    $redirectParams = $_GET;
    unset($redirectParams['ajax']);
    $queryString = http_build_query($redirectParams);
    $targetUrl = '../admin.php' . ($queryString ? '?' . $queryString : '') . '#cajas';
    header('Location: ' . $targetUrl);
    exit;
}

$cajas = [];
$lookupSucursales = [];
$lookupUsuarios = [];
$error = null;
$data = [];
$token = $_GET['token'] ?? ($_COOKIE['userToken'] ?? null);

if (!$token) {
    $error = 'No se encontró el token de sesión. Inicia sesión nuevamente.';
} else {
    // 1. LISTA DE CAJAS
    $baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/cajas';

    $query = [];
    if (!empty($_GET['estado'])) $query['estado'] = $_GET['estado'];
    if (!empty($_GET['empresaId'])) $query['empresaId'] = $_GET['empresaId'];
    if (!empty($_GET['sucursalId'])) $query['sucursalId'] = $_GET['sucursalId'];

    $requestUrl = $baseUrl . (empty($query) ? '' : '?' . http_build_query($query));

    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => $requestUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token],
    ]);

    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    curl_close($curl);

    if ($httpCode === 200) {
        $data = json_decode($response, true);
        $cajas = $data['data'] ?? [];
    } else {
        $error = "Error al obtener cajas (HTTP $httpCode)";
    }

    // 2. LOOKUP SUCURSALES
    $urlSucursales = "http://turistas.spring.informaticapp.com:2410/api/v1/sucursales";

    $curl2 = curl_init();
    curl_setopt_array($curl2, [
        CURLOPT_URL => $urlSucursales,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token],
    ]);

    $responseSuc = curl_exec($curl2);
    $codeSuc = curl_getinfo($curl2, CURLINFO_HTTP_CODE);
    curl_close($curl2);

    if ($codeSuc === 200) {
        $listaSuc = json_decode($responseSuc, true);

        if (!empty($listaSuc['data'])) {
            foreach ($listaSuc['data'] as $s) {
                $lookupSucursales[$s['idSucursal']] = $s['nombreSucursal'];
            }
        }
    }

    // 3. LOOKUP USUARIOS
    $urlUsuarios = "http://turistas.spring.informaticapp.com:2410/api/v1/usuarios";

    $curl3 = curl_init();
    curl_setopt_array($curl3, [
        CURLOPT_URL => $urlUsuarios,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token],
    ]);

    $responseUsers = curl_exec($curl3);
    $codeUsers = curl_getinfo($curl3, CURLINFO_HTTP_CODE);
    curl_close($curl3);

    if ($codeUsers === 200) {
        $listaUsers = json_decode($responseUsers, true);

        if (!empty($listaUsers['data'])) {
            foreach ($listaUsers['data'] as $u) {
                $nombreCompleto = trim(($u['nombre'] ?? '') . ' ' . ($u['apellido'] ?? ''));
                $lookupUsuarios[$u['idUsuario']] = $nombreCompleto;
            }
        }
    }
}

    // RENDER TABLA
ob_start();
?>
<style>/* --------------------------------- */
/* ESTILOS DE TABLA Y HEADER         */
/* --------------------------------- */
.caja-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 15px 20px;
    background: #f8f9fa;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}



.caja-header .acciones-header {
    display: flex;
    gap: 12px;
}

.caja-header input {
    padding: 10px 12px;
    border-radius: 8px;
    border: 1px solid #ccc;
    width: 240px;
    transition: 0.2s;
}
.caja-header input:focus {
    border-color: #0d6efd;
    box-shadow: 0 0 6px rgba(13,110,253,0.3);
    outline: none;
}

.btn-crear {
    padding: 10px 16px;
    background: #198754;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 600;
    box-shadow: 0 2px 6px rgba(0,0,0,0.15);
    transition: 0.3s;
}
.btn-crear:hover {
    background: #146c43;
    transform: translateY(-2px);
}

/* --------------------------------- */
/* TABLA                             */
/* --------------------------------- */
.tabla-cajas {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    font-size: 14px;
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 6px 15px rgba(0,0,0,0.08);
}

.tabla-cajas thead {
    background: #0d6efd;
    color: #fff;
    font-weight: 600;
}

.tabla-cajas th, .tabla-cajas td {
    padding: 14px 16px;
    text-align: left;
}

.tabla-cajas tbody tr {
    border-bottom: 1px solid #eee;
    transition: all 0.25s ease-in-out;
}
.tabla-cajas tbody tr:hover {
    background: #e7f1ff;
    transform: translateX(2px);
}

.acciones-btns i {
    font-size: 16px;
    margin-right: 8px;
    cursor: pointer;
    transition: 0.2s;
}
.acciones-btns i:hover {
    transform: scale(1.2);
}
.acciones-btns i.btn-arquear { color: #198754; }
.acciones-btns i.btn-cerrar { color: #dc3545; }

/* ESTADOS */
.estado {
    padding: 6px 12px;
    border-radius: 16px;
    font-weight: 600;
    font-size: 13px;
    color: #fff;
    text-align: center;
    display: inline-block;
}
.estado.abierta { background: #198754; }
.estado.cerrada { background: #dc3545; }

/* --------------------------------- */
/* ESTILOS MODAL CREAR CAJA          */
/* --------------------------------- */
#modalCrearCaja, #modalMovimientos, #modalAgregarMovimiento, #modalCerrarCaja {
    display: none;
    position: fixed;
    top:0; left:0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
    justify-content: center;
    align-items: center;
    z-index: 9999;
    animation: fadeIn 0.25s ease-in-out;
}

.modal-box {
    background:white;
    width: 420px;
    max-width: 90%;
    padding: 25px;
    border-radius: 14px;
    box-shadow:0 8px 20px rgba(0,0,0,0.25);
    animation: scaleUp 0.2s ease-in-out;
}

@keyframes fadeIn {
    from {opacity:0;}
    to {opacity:1;}
}

@keyframes scaleUp {
    from {transform: scale(0.95);}
    to {transform: scale(1);}
}

.modal-box h3 {
    margin-top:0;
    color:#0d6efd;
    font-size: 20px;
    font-weight: 700;
}

.modal-box input, 
.modal-box select, 
.modal-box textarea {
    width: 100%;
    padding: 10px 12px;
    margin-bottom: 14px;
    border-radius: 8px;
    border: 1px solid #ccc;
    transition: 0.2s;
}
.modal-box input:focus, .modal-box select:focus, .modal-box textarea:focus {
    border-color: #0d6efd;
    box-shadow: 0 0 6px rgba(13,110,253,0.3);
    outline: none;
}

.btn-save {
    background:#198754;
    color:white;
    border:none;
    padding:10px 16px;
    border-radius:8px;
    cursor:pointer;
    font-weight: 600;
    transition: 0.3s;
}
.btn-save:hover {
    background: #146c43;
    transform: translateY(-2px);
}

.btn-cancel {
    background:#dc3545;
    color:white;
    border:none;
    padding:10px 16px;
    border-radius:8px;
    cursor:pointer;
    font-weight: 600;
    transition: 0.3s;
}
.btn-cancel:hover {
    background: #a71d2a;
    transform: translateY(-2px);
}

.btn-filtrar:hover {
    background: linear-gradient(135deg, #0a58ca, #084298);
    transform: translateY(-2px);
    box-shadow: 0 6px 14px rgba(13, 110, 253, 0.5);
}

.btn-filtrar i {
    font-size: 16px;
}

</style>

    <?php
    if (!empty($error)) {
        echo "<div style='color:red;font-weight:bold;'>$error</div>";
        return;
    }

    if (empty($cajas)) {
        echo "<p>No hay cajas registradas.</p>";
    }
    ?>

    <!-- HEADER -->
    <div class="caja-header">
        <h2 class="section-title">Gestión de Cajas</h2>

        <div class="acciones-header">
            <input type="text" id="buscadorCajas" placeholder="Buscar caja...">
            <button class="btn-crear" onclick="crearCaja()">+ Crear Caja</button>
        </div>
    </div>
<div style="display:flex; align-items:center; gap:8px; margin-bottom:15px;">
    <select id="filtroSucursal" style="padding:10px 12px; border-radius:8px; border:1px solid #ccc; transition:0.2s;">
        <option value="">Todas las sucursales</option>
        <?php foreach ($lookupSucursales as $id => $nombre): ?>
            <option value="<?= $id ?>"><?= $nombre ?></option>
        <?php endforeach; ?>
    </select>

</div>


    <!-- TABLA -->
    <table class="tabla-cajas">
        <thead>
            <tr>
                <th># Caja</th>
                <th>Sucursal</th>
                <th>Usuario Apertura</th>
                <th>Monto Inicial</th>
                <th>Saldo Actual</th>
                <th>Estado</th>
                <th>Fecha Apertura</th>
                <th>Observaciones</th>
                <th>Acciones</th>
            </tr>
        </thead>

    <tbody id="listaCajas">
        <?php $contador = 1; ?>
        <?php foreach ($cajas as $caja): ?>
            <tr>
<td 
    data-codigo="<?= sprintf("CAJ-%03d", $contador); ?>">
    <?= sprintf("CAJ-%03d", $contador++); ?>
</td>
<td data-id="<?= $caja['idSucursal'] ?>"><?= htmlspecialchars($lookupSucursales[$caja['idSucursal']] ?? 'Sin nombre'); ?></td>
                <td><?= htmlspecialchars($lookupUsuarios[$caja['idUsuarioApertura']] ?? 'Desconocido'); ?></td>
                <td>S/. <?= number_format($caja['montoInicial'], 2); ?></td>
                <td>S/. <?= number_format($caja['saldoActual'], 2); ?></td>
                <td>
                    <span class="estado <?= strtolower($caja['estado']) ?>">
                        <?= htmlspecialchars($caja['estado']); ?>
                    </span>
                </td>
                <td><?= htmlspecialchars($caja['fechaApertura'] . ' ' . $caja['horaApertura']); ?></td>
                <td><?= htmlspecialchars($caja['observaciones']); ?></td>
                <td class="acciones-btns">
<i class="fas fa-cash-register btn-arquear"
   onclick="arquearCaja(
       <?= $caja['idCaja'] ?>,
       this.closest('tr').querySelector('td[data-codigo]').dataset.codigo
   )">
</i>
                    <?php if($caja['estado'] === 'Cerrada'): ?>
    <i class="fas fa-lock btn-cerrar" 
    title="Caja cerrada" 
    onclick="abrirCajaExistente(<?= $caja['idCaja'] ?>, '<?= $caja['estado'] ?>')">
    </i>                <?php else: ?>
                        <i class="fas fa-lock-open btn-cerrar" onclick="cerrarCaja(<?= $caja['idCaja'] ?>)" title="Cerrar caja"></i>
                    <?php endif; ?>
                    <i class="fas fa-money-bill-wave btn-mov" onclick="movimientosCaja(<?= $caja['idCaja'] ?>)"></i>
                </td>
            </tr>
        <?php endforeach; ?>
    </tbody>


    </table>

    <div id="modalCrearCaja">
        <div class="modal-box">
            <h3>Abrir Caja</h3>

            <label>Sucursal:</label>
            <select id="sucursalCrear">
                <?php foreach ($lookupSucursales as $id => $nombre): ?>
                    <option value="<?= $id ?>"><?= $nombre ?></option>
                <?php endforeach; ?>
            </select>


            <label>Monto Inicial:</label>
            <input type="number" id="montoCrear">

            <label>Observaciones:</label>
            <textarea id="obsCrear"></textarea>

            <button class="btn-save" onclick="guardarCaja()">Guardar</button>
            <button class="btn-cancel" onclick="cerrarModal()">Cancelar</button>
        </div>
    </div>

    <!-- ===================================== -->
    <!--        MODAL LISTA DE MOVIMIENTOS     -->
    <!-- ===================================== -->
    <!-- MODAL LISTA DE MOVIMIENTOS -->
    <div id="modalMovimientos" style="
        display:none; position:fixed; top:0; left:0; width:100%; height:100%;
        background:rgba(0,0,0,0.5); justify-content:center; align-items:flex-start;
        padding-top:50px; z-index:9999;">
        
        <div class="modal-box" style="width:900px; max-height:85vh; overflow-y:auto; position:relative;">
            <h3>Movimientos de Caja <span id="movIdCaja"></span></h3>

            <!-- Botón Agregar Movimiento -->
            <button class="btn-crear" style="position:absolute; top:20px; right:20px;" onclick="abrirAgregarMovimiento()">
                + Agregar Movimiento
            </button>

            <!-- Tabla de Movimientos -->
            <table class="tabla-cajas" style="margin-top:50px;">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Tipo</th>
                        <th>Monto</th>
                        <th>Descripción</th>
                        <th>Fecha</th>
                    </tr>
                </thead>
                <tbody id="listaMovimientos"></tbody>
            </table>

            <button class="btn-cancel" onclick="cerrarMovimientos()" style="margin-top:10px;">Cerrar</button>
        </div>
    </div>

    <!-- MODAL AGREGAR MOVIMIENTO -->
    <div id="modalAgregarMovimiento" style="
        display:none; position:fixed; top:0; left:0; width:100%; height:100%;
        background:rgba(0,0,0,0.5); justify-content:center; align-items:center;
        z-index:10000;">
        
        <div class="modal-box" style="width:400px;">
            <h3>Agregar Movimiento</h3>

            <label>Tipo:</label>
            <select id="tipoMovimiento">
                <option value="Ingreso">Ingreso</option>
                <option value="Egreso">Egreso</option>
            </select>

            <label>Monto:</label>
            <input type="number" id="montoMovimiento" step="0.01">

            <label>Descripción:</label>
            <textarea id="descMovimiento"></textarea>

            <div style="display:flex; justify-content:space-between;">
                <button class="btn-save" onclick="guardarMovimiento()">Guardar</button>
                <button class="btn-cancel" onclick="cerrarAgregarMovimiento()">Cancelar</button>
            </div>
        </div>
    </div>
    <!-- ===================================== -->
    <!--           MODAL CERRAR CAJA           -->
    <!-- ===================================== -->
    <div id="modalCerrarCaja" style="
        display:none; position:fixed; top:0; left:0; width:100%; height:100%;
        background:rgba(0,0,0,0.5); justify-content:center; align-items:center;
        z-index:10001;
    ">
        <div class="modal-box" style="width:400px;">
            <h3>Cerrar Caja <span id="cerrarCajaId"></span></h3>

            <label>Monto de Cierre:</label>
            <input type="number" id="montoCierreModal" step="0.01">

            <label>Observaciones:</label>
            <textarea id="obsCierreModal" placeholder="Opcional"></textarea>

            <div style="display:flex; justify-content:space-between;">
                <button class="btn-save" onclick="guardarCerrarCaja()">Cerrar Caja</button>
                <button class="btn-cancel" onclick="cerrarCerrarCaja()">Cancelar</button>
            </div>
        </div>
    </div>
<!-- ============================= -->
<!--      MODAL ARQUEO DE CAJA    -->
<!-- ============================= -->
<div id="modalArqueo" style="
    display:none; position:fixed; top:0; left:0;
    width:100%; height:100%; background:rgba(0,0,0,0.5);
    justify-content:center; align-items:flex-start; padding-top:40px; z-index:10002;
">
    <div class="modal-box" style="width:700px;">
        <h3>Arqueo de  #<span id="arqueoIdCaja"></span></h3>

        <hr>

        <h4>Resumen de Movimientos</h4>

        <table class="tabla-cajas" style="margin-top:10px;">
            <thead>
                <tr>
                    <th>Monto Inicial</th>
                    <th>Total Ingresos</th>
                    <th>Total Egresos</th>
                    <th>Saldo Actual</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td id="arqueoMontoInicial"></td>
                    <td id="arqueoIngresos"></td>
                    <td id="arqueoEgresos"></td>
                    <td id="arqueoSaldo"></td>
                </tr>
            </tbody>
        </table>

        <button class="btn-cancel" onclick="cerrarArqueo()" style="margin-top:10px;">Cerrar</button>
    </div>
</div>


    <script>


    const TOKEN = "<?= $token ?>";


    document.getElementById("buscadorCajas").addEventListener("keyup", function() {
        let value = this.value.toLowerCase();
        let filas = document.querySelectorAll("#listaCajas tr");
        filas.forEach(fila => {
            fila.style.display = fila.textContent.toLowerCase().includes(value) ? "" : "none";
        });
    });
function abrirCajaExistente(idCaja) {
    Swal.fire({
        title: 'Reabrir Caja',
        text: `¿Desea reabrir la caja #${idCaja}?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, abrir',
        cancelButtonText: 'Cancelar'
    }).then(result => {
        if (result.isConfirmed) {
            fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer <?= $token ?>"
                },
                body: JSON.stringify({
                    estado: "Abierta",
                    observaciones: "Reapertura de caja"
                })
            })
            .then(r => r.json())
            .then(json => {
                if (json.success) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Caja reabierta',
                        text: json.message
                    }).then(() => {
                        location.reload(); // ✅ RECARGA TOTAL
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Error',
                        text: json.message
                    });
                }
            });
        }
    });
}
function recargarTablaCajas(sucursalId = '') {
    let url = 'cajas.php?ajax=1';
    if (sucursalId) url += '&sucursalId=' + sucursalId;

    fetch(url, { headers: { "Authorization": "Bearer " + TOKEN } })
        .then(r => r.text())
        .then(html => {
            // Parsear el HTML recibido
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // Obtener solo el tbody de la nueva tabla
            const nuevoTbody = doc.querySelector('#listaCajas');
            if (nuevoTbody) {
                document.querySelector('#listaCajas').innerHTML = nuevoTbody.innerHTML;
            }

            // Opcional: re-aplicar filtros y buscador
            document.getElementById("buscadorCajas")?.addEventListener("keyup", function() {
                let value = this.value.toLowerCase();
                let filas = document.querySelectorAll("#listaCajas tr");
                filas.forEach(fila => {
                    fila.style.display = fila.textContent.toLowerCase().includes(value) ? "" : "none";
                });
            });

            document.getElementById("filtroSucursal")?.addEventListener("change", function() {
                recargarTablaCajas(this.value);
            });
        })
        .catch(err => console.error("Error recargando tabla:", err));
}

function cerrarMovimientos() {
    document.getElementById('modalMovimientos').style.display = 'none';
    // Limpiar tabla de movimientos si quieres
    document.getElementById('listaMovimientos').innerHTML = '';
}

document.getElementById("filtroSucursal")?.addEventListener("change", function() {
    const sucursalId = this.value;
    const filas = document.querySelectorAll("#listaCajas tr");
    filas.forEach(fila => {
        const filaSucursal = fila.cells[1].getAttribute("data-id");
        fila.style.display = (!sucursalId || filaSucursal == sucursalId) ? "" : "none";
    });
});

    function reabrirCaja(idCaja) {
        const data = {
            sucursalId: 1, // opcional, puedes traerlo de la caja actual
            estado: "Abierta",
            montoInicial: parseFloat(document.getElementById("montoCrear").value),
            observaciones: document.getElementById("obsCrear").value
        };

        // Usar POST /cajas como "reapertura"
        fetch("http://turistas.spring.informaticapp.com:2410/api/v1/cajas", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer <?= $token ?>"
            },
            body: JSON.stringify(data)
        })
        .then(r => r.json())
        .then(json => {
            if(json.success) {
                Swal.fire({
                    icon: 'success',
                    title: 'Caja reabierta',
                    text: json.message,
                    confirmButtonColor: '#0d6efd'
                }).then(() => {
                    cerrarModal();
    actualizarTablaCajas(); // 🔥 Recarga automática
s                });
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: json.message || 'No se pudo reabrir la caja',
                    confirmButtonColor: '#dc3545'
                });
            }
        })
        .catch(err => console.error('Error al reabrir caja', err));
    }

    function crearCaja() {
        document.getElementById("modalCrearCaja").style.display = "flex";
    }
    

    function cerrarModal() {
        document.getElementById("modalCrearCaja").style.display = "none";
    }

function guardarCaja() {
    const data = {
        sucursalId: document.getElementById("sucursalCrear").value,
        estado: "Abierta", // siempre abrir
        montoInicial: parseFloat(document.getElementById("montoCrear").value),
        observaciones: document.getElementById("obsCrear").value
    };

    fetch("http://turistas.spring.informaticapp.com:2410/api/v1/cajas", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer <?= $token ?>"
        },
        body: JSON.stringify(data)
    })
    .then(async r => {
        const json = await r.json();

        if (r.ok && json.success) {
            Swal.fire({
                icon: 'success',
                title: 'Caja abierta',
                text: json.message,
                confirmButtonColor: '#0d6efd'
            }).then(() => {

    cerrarModal();        // ✅ cierra modal
    location.reload();    // ✅ recarga TODO (tabla, estado, etc)

            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: json.message || 'No se pudo abrir la caja',
                confirmButtonColor: '#dc3545'
            });
        }
    })
    .catch(err => {
        Swal.fire({
            icon: 'error',
            title: 'Error de conexión',
            text: 'No se pudo conectar con el servidor',
            confirmButtonColor: '#dc3545'
        });
    });
}

function actualizarTablaCajas() {
    fetch("inicio/CAJAS/cajas.php?ajax=1")
        .then(res => res.text())
        .then(html => {
            const temp = document.createElement("div");
            temp.innerHTML = html;

            // 🔥 Reemplazar SOLO el tbody
            const nuevoTbody = temp.querySelector("#listaCajas");
            const actualTbody = document.querySelector("#listaCajas");

            if (nuevoTbody && actualTbody) {
                actualTbody.innerHTML = nuevoTbody.innerHTML;
            }
        })
        .catch(err => console.log("Error al recargar cajas:", err));
}

function movimientosCaja(idCaja) {
    document.getElementById("modalMovimientos").style.display = "flex";
    document.getElementById("movIdCaja").innerText = `#${idCaja}`;
    cargarMovimientos(idCaja);
}
function abrirAgregarMovimiento() {
    document.getElementById("modalAgregarMovimiento").style.display = "flex";
}

function cargarMovimientos(idCaja) {
    fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}/movimientos`, {
        headers: { "Authorization": "Bearer <?= $token ?>" }
    })
    .then(r => r.json())
    .then(json => {
        const tbody = document.getElementById("listaMovimientos");
        tbody.innerHTML = "";
        if (json.success && Array.isArray(json.data)) {
            json.data.forEach(m => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${m.idMovimiento}</td>
                    <td>${m.tipoMovimiento}</td>
                    <td>S/. ${parseFloat(m.monto).toFixed(2)}</td>
                    <td>${m.descripcion}</td>
                    <td>${new Date(m.fechaHora).toLocaleString()}</td>
                `;
                tbody.appendChild(tr);
            });
        }
    })
    .catch(err => console.error("Error cargando movimientos", err));
}


function cerrarAgregarMovimiento() {
    document.getElementById("modalAgregarMovimiento").style.display = "none";
    document.getElementById("tipoMovimiento").value = "Ingreso";
    document.getElementById("montoMovimiento").value = "";
    document.getElementById("descMovimiento").value = "";
}


function guardarMovimiento() {
    const idCaja = document.getElementById("movIdCaja").innerText.replace("#", "");
    const data = {
        tipoMovimiento: document.getElementById("tipoMovimiento").value,
        monto: parseFloat(document.getElementById("montoMovimiento").value),
        descripcion: document.getElementById("descMovimiento").value,
    };

    fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}/movimientos`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer <?= $token ?>"
        },
        body: JSON.stringify(data)
    })
    .then(r => r.json())
    .then(json => {
        if (json.success) {
            Swal.fire({
                icon: 'success',
                title: 'Movimiento registrado',
                text: json.message,
                confirmButtonColor: '#0d6efd'
            }).then(() => {
                cerrarAgregarMovimiento();
                cargarMovimientos(idCaja); // recarga la tabla de movimientos
                cargarSeccion('#cajas');   // actualiza saldo de la caja
            });
        } else {

            // 🔴 NORMALIZAR MENSAJE DE ERROR
            let mensaje = json.message || 'No se pudo registrar el movimiento';

            if (mensaje.includes('El saldo de caja no puede ser negativo')) {
                mensaje = 'El saldo de caja no es suficiente';
            }

            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: mensaje,
                confirmButtonColor: '#dc3545'
            });
        }
    })
    .catch(err => {
        console.error('Error registrando movimiento', err);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error de conexión al registrar movimiento',
            confirmButtonColor: '#dc3545'
        });
    });
}

   
    function cerrarCaja(idCaja) {
        document.getElementById('cerrarCajaId').innerText = idCaja;
        document.getElementById('montoCierreModal').value = '';
        document.getElementById('obsCierreModal').value = '';
        document.getElementById('modalCerrarCaja').style.display = 'flex';
    }

    // Cerrar modal
    function cerrarCerrarCaja() {
        document.getElementById('modalCerrarCaja').style.display = 'none';
    }

function guardarCerrarCaja() {
    const idCaja = document.getElementById('cerrarCajaId').innerText;
    const monto = parseFloat(document.getElementById('montoCierreModal').value);
    const obs = document.getElementById('obsCierreModal').value;

    if (isNaN(monto)) {
        Swal.fire({
            icon: 'warning',
            title: 'Atención',
            text: 'Ingrese un monto válido'
        });
        return;
    }

    fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}/cerrar`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer <?= $token ?>"
        },
        body: JSON.stringify({
            montoCierre: monto,
            observaciones: obs
        })
    })
    .then(r => r.json())
    .then(json => {
        if (json.success) {
            Swal.fire({
                icon: 'success',
                title: 'Caja cerrada',
                text: 'La caja se cerró correctamente',
                confirmButtonColor: '#0d6efd'
            }).then(() => {
                location.reload(); // ✅ RECARGA TOTAL
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: json.message || 'No se pudo cerrar la caja'
            });
        }
    })
    .catch(() => {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error de conexión'
        });
    });
}

function cargarSeccion(selector = '#cajas', sucursalId = '') {
    const contenedor = document.querySelector(selector);
    let url = 'cajas.php?ajax=1';
    if (sucursalId) url += '&sucursalId=' + sucursalId;

    fetch(url, { headers: { "Authorization": "Bearer " + TOKEN } })
        .then(r => r.text())
        .then(html => {
            contenedor.innerHTML = html;

            // REAPLICAR EVENTOS
            aplicarEventosCajas();
        })
        .catch(err => console.error("Error cargando sección:", err));
}

function aplicarEventosCajas() {
    // Buscador
    document.getElementById("buscadorCajas")?.addEventListener("keyup", function() {
        let value = this.value.toLowerCase();
        document.querySelectorAll("#listaCajas tr").forEach(fila => {
            fila.style.display = fila.textContent.toLowerCase().includes(value) ? "" : "none";
        });
    });

    // Filtro por sucursal
    document.getElementById("filtroSucursal")?.addEventListener("change", function() {
        cargarSeccion('#cajas', this.value);
    });

    // Botón crear caja
    document.querySelector(".btn-crear")?.addEventListener("click", crearCaja);
}



function arquearCaja(idCaja, codigoCaja) {
    const token = TOKEN;

    // 1. OBTENER DATOS DE LA CAJA
    fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}`, {
        headers: { "Authorization": "Bearer " + token }
    })
    .then(r => r.json())
    .then(cajaResp => {

        if (!cajaResp.data) {
            alert("No se pudo obtener la caja");
            return;
        }

        const caja = cajaResp.data;

        // PINTAR DATOS BÁSICOS
document.getElementById("arqueoIdCaja").textContent = codigoCaja;
        document.getElementById("arqueoMontoInicial").textContent = "S/. " + Number(caja.montoInicial).toFixed(2);
        document.getElementById("arqueoSaldo").textContent = "S/. " + Number(caja.saldoActual).toFixed(2);

        // 2. OBTENER MOVIMIENTOS
        return fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${idCaja}/movimientos`, {
            headers: { "Authorization": "Bearer " + token }
        })
        .then(r => r.json())
        .then(movResp => {
            
            let ingresos = 0;
            let egresos = 0;

            if (movResp.data && Array.isArray(movResp.data)) {
                movResp.data.forEach(m => {
                    // CORRECCIÓN: usar tipoMovimiento en vez de tipo
                    if (m.tipoMovimiento === "Ingreso") ingresos += Number(m.monto);
                    else if (m.tipoMovimiento === "Egreso") egresos += Number(m.monto);
                });
            }

            // 3. PINTAR TOTALES
            document.getElementById("arqueoIngresos").textContent = "S/. " + ingresos.toFixed(2);
            document.getElementById("arqueoEgresos").textContent = "S/. " + egresos.toFixed(2);

            // 4. ABRIR MODAL
            document.getElementById("modalArqueo").style.display = "flex";
        });

    })
    .catch(err => {
        console.error("Error Arqueo:", err);
    });
}

document.addEventListener("DOMContentLoaded", function () {
    const buscador = document.getElementById("buscadorCajas");
    const tabla = document.getElementById("listaCajas");

    if (!buscador || !tabla) return;

    buscador.addEventListener("keyup", function () {
        const texto = buscador.value.toLowerCase();
        const filas = tabla.getElementsByTagName("tr");

        for (let i = 0; i < filas.length; i++) {
            const fila = filas[i];
            const contenidoFila = fila.innerText.toLowerCase();

            if (contenidoFila.includes(texto)) {
                fila.style.display = "";
            } else {
                fila.style.display = "none";
            }
        }
    });
});
function cerrarArqueo() {
    document.getElementById("modalArqueo").style.display = "none";
}

    </script>

    <?php
    $content = ob_get_clean();
    header('Content-Type: text/html; charset=utf-8');
    echo $content;
    exit;
    ?>
