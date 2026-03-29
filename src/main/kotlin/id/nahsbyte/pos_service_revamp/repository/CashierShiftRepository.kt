package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.CashierShift
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CashierShiftRepository : JpaRepository<CashierShift, Long> {
    fun findByMerchantId(merchantId: Long): List<CashierShift>
    fun findByMerchantIdAndOutletId(merchantId: Long, outletId: Long): List<CashierShift>
    fun findByMerchantIdAndOutletIdAndStatus(merchantId: Long, outletId: Long, status: String): Optional<CashierShift>
    fun findByMerchantIdAndUserIdAndStatus(merchantId: Long, userId: Long, status: String): Optional<CashierShift>
}
