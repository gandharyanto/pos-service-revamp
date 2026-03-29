package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Outlet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OutletRepository : JpaRepository<Outlet, Long> {
    fun findByMerchantIdAndIsDefaultTrue(merchantId: Long): Optional<Outlet>
    fun findAllByMerchantId(merchantId: Long): List<Outlet>
}
