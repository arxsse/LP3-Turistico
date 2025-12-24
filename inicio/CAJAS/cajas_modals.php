<!-- ========================= -->
<!--     MODAL CREAR CAJA      -->
<!-- ========================= -->
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
        <input type="number" id="montoCrear" step="0.01">

        <label>Observaciones:</label>
        <textarea id="obsCrear"></textarea>

        <button class="btn-save" onclick="guardarCaja()">Guardar</button>
        <button class="btn-cancel" onclick="cerrarModal()">Cancelar</button>
    </div>
</div>

<!-- =============================== -->
<!--   MODAL REGISTRAR MOVIMIENTO    -->
<!-- =============================== -->
<div id="modalMovimientoCaja" class="modal-overlay">
    <div class="modal-box">
        <h3>Registrar Movimiento</h3>

        <input type="hidden" id="movCajaId">

        <label>Tipo de Movimiento:</label>
        <select id="movTipo">
            <option value="INGRESO">Ingreso</option>
            <option value="EGRESO">Egreso</option>
        </select>

        <label>Monto:</label>
        <input type="number" id="movMonto">

        <label>Descripción:</label>
        <textarea id="movDescripcion"></textarea>

        <div class="modal-actions">
            <button class="btn-save" onclick="guardarMovimiento()">Registrar</button>
            <button class="btn-cancel" onclick="cerrarModal('modalMovimientoCaja')">Cancelar</button>
        </div>
    </div>
</div>
<!-- ========================= -->
<!--      MODAL ARQUEO         -->
<!-- ========================= -->
<div id="modalArqueoCaja" class="modal-overlay">
    <div class="modal-box">
        <h3>Arqueo de Caja</h3>

        <input type="hidden" id="arqueoCajaId">

        <label>Saldo Actual del Sistema:</label>
        <input type="number" id="arqueoSistema" disabled>

        <label>Monto Contado:</label>
        <input type="number" id="arqueoContado">

        <label>Diferencia:</label>
        <input type="number" id="arqueoDiferencia" disabled>

        <div class="modal-actions">
            <button class="btn-save" onclick="guardarArqueo()">Guardar Arqueo</button>
            <button class="btn-cancel" onclick="cerrarModal('modalArqueoCaja')">Cancelar</button>
        </div>
    </div>
</div>
<!-- ========================= -->
<!--      MODAL CERRAR         -->
<!-- ========================= -->
<div id="modalCerrarCaja" class="modal-overlay">
    <div class="modal-box">
        <h3>Cerrar Caja</h3>

        <input type="hidden" id="cerrarCajaId">

        <label>Usuario que cierra:</label>
        <select id="cerrarUsuario"></select>

        <label>Monto Contado Final:</label>
        <input type="number" id="cerrarMonto">

        <label>Observaciones:</label>
        <textarea id="cerrarObs"></textarea>

        <div class="modal-actions">
            <button class="btn-save" onclick="guardarCierreCaja()">Cerrar Caja</button>
            <button class="btn-cancel" onclick="cerrarModal('modalCerrarCaja')">Cancelar</button>
        </div>
    </div>
</div>
<!-- ================================ -->
<!--     MODAL LISTA MOVIMIENTOS      -->
<!-- ================================ -->
<div id="modalListaMovimientos" class="modal-overlay">
    <div class="modal-box" style="width:600px;">
        <h3>Movimientos de Caja</h3>

        <div id="tablaMovimientos"></div>

        <div class="modal-actions">
            <button class="btn-cancel" onclick="cerrarModal('modalListaMovimientos')">Cerrar</button>
        </div>
    </div>
</div>
