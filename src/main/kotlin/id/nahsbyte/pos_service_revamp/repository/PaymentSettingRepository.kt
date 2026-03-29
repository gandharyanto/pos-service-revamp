package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PaymentSetting
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentSettingRepository : JpaRepository<PaymentSetting, Long> {
    fun findByMerchantId(merchantId: Long): Optional<PaymentSetting>
}
