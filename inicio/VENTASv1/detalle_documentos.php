<?php
$ventaId = isset($_GET['id']) ? intval($_GET['id']) : null;
if (!$ventaId) {
    echo 'ID de venta requerido';
    exit;
}

// Placeholder: en el futuro se listarán facturas, recibos y otros documentos asociados a la venta
if (isset($_GET['ajax']) && $_GET['ajax'] == '1') {
    header('Content-Type: text/html; charset=utf-8');
    ?>
    <div class="content-header">
        <div class="card">
            <div class="card-header"><h2 class="section-title">Documentos de la Venta #<?php echo htmlspecialchars($ventaId); ?></h2></div>
            <div class="card-body">
                <p>No hay documentos asociados todavía. Aquí se mostrarán facturas, comprobantes y otros archivos relacionados con la venta.</p>
                <div>
                    <button class="btn btn-primary" onclick="if(typeof loadVentasContent==='function') loadVentasContent(); else window.location.href='ventas.php?ajax=1'">Volver</button>
                </div>
            </div>
        </div>
    </div>
    <?php
    exit;
}

?>
