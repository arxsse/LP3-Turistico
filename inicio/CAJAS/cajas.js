
document.addEventListener("DOMContentLoaded", function () {

    const modal = document.getElementById("modalCrearCaja");
    const overlay = document.getElementById("modalOverlay");
    const btnOpen = document.getElementById("btnAbrirModalCaja");
    const btnClose = document.getElementById("btnCerrarModalCaja");

    // Abrir modal
    btnOpen?.addEventListener("click", () => {
        modal.style.display = "flex";
        overlay.style.display = "block";
    });

    // Cerrar modal
    btnClose?.addEventListener("click", cerrarModal);

    // Cerrar al hacer click fuera
    overlay?.addEventListener("click", cerrarModal);
});

// ===============================
// CERRAR MODAL
// ===============================
function cerrarModal() {
    document.getElementById("modalCrearCaja").style.display = "none";
    document.getElementById("modalOverlay").style.display = "none";
}

