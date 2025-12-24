<div class="caja-header">
    <h2>Gestión de Cajas</h2>

    <div class="acciones-header">
        <input type="text" id="buscadorCajas" placeholder="Buscar...">
        <button class="btn-crear" onclick="abrirModal('modalCrearCaja')">+ Crear Caja</button>
    </div>
</div>

<table class="tabla-cajas">
    <thead>
        <tr>
            <th>ID</th>
            <th>Sucursal</th>
            <th>Usuario Apertura</th>
            <th>Monto Inicial</th>
            <th>Saldo Actual</th>
            <th>Estado</th>
            <th>Fecha</th>
            <th>Hora</th>
            <th>Acciones</th>
        </tr>
    </thead>

    <tbody id="listaCajas">

    <?php foreach ($cajas as $cx): ?>
        <tr>
            <td><?= $cx['idCaja'] ?></td>
            <td><?= $lookupSucursales[$cx['idSucursal']] ?? '---' ?></td>
            <td><?= $lookupUsuarios[$cx['idUsuarioApertura']] ?? '---' ?></td>
            <td><?= $cx['montoInicial'] ?></td>
            <td><?= $cx['saldoActual'] ?></td>
            <td><?= $cx['estado'] ?></td>
            <td><?= $cx['fechaApertura'] ?></td>
            <td><?= $cx['horaApertura'] ?></td>

            <td class="acciones-btns">
                <i class="fas fa-edit" onclick="editarCaja(<?= $cx['idCaja'] ?>)"></i>
                <i class="fas fa-cash-register" onclick="abrirArqueo(<?= $cx['idCaja'] ?>)"></i>
                <i class="fas fa-lock" onclick="abrirCierre(<?= $cx['idCaja'] ?>)"></i>
                <i class="fas fa-money-bill-wave" onclick="abrirMovimientos(<?= $cx['idCaja'] ?>)"></i>
            </td>
        </tr>
    <?php endforeach; ?>

    </tbody>
</table>

echo "<link rel='stylesheet' href='/sistematuristico/inicio/CAJAS/cajas.css'>";
echo "<script src='/sistematuristico/inicio/CAJAS/cajas.js'></script>";
