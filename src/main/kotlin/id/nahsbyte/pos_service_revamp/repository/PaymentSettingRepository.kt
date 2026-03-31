package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PaymentSetting
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentSettingRepository : JpaRepository<PaymentSetting, Long> {
    /** Default setting (berlaku semua outlet) */
    fun findByMerchantId(merchantId: Long): Optional<PaymentSetting>
    /** Semua setting merchant ini (default + per-outlet overrides) */
    fun findAllByMerchantId(merchantId: Long): List<PaymentSetting>
    fun findByMerchantIdAndOutletId(merchantId: Long, outletId: Long): Optional<PaymentSetting>
    fun findByMerchantIdAndOutletIdIsNull(merchantId: Long): Optional<PaymentSetting>
}
