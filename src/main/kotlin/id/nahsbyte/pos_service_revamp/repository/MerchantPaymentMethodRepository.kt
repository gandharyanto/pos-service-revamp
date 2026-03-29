package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.MerchantPaymentMethod
import org.springframework.data.jpa.repository.JpaRepository

interface MerchantPaymentMethodRepository : JpaRepository<MerchantPaymentMethod, Long> {
    fun findAllByMerchantIdAndIsEnabledTrue(merchantId: Long): List<MerchantPaymentMethod>
}
