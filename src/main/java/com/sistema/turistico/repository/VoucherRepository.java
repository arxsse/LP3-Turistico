package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByReserva_IdReserva(Long reservaId);
    Optional<Voucher> findByCodigoQr(String codigoQr);

    List<Voucher> findByReserva_Empresa_IdEmpresa(Long empresaId);
}
