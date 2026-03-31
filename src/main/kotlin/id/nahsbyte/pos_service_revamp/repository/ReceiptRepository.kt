package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.ReceiptTemplate
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReceiptRepository : JpaRepository<ReceiptTemplate, Long> {
    fun findAllByMerchantId(merchantId: Long): List<ReceiptTemplate>
    fun findByMerchantIdAndOutletIdIsNull(merchantId: Long): Optional<ReceiptTemplate>
    fun findByMerchantIdAndOutletId(merchantId: Long, outletId: Long): Optional<ReceiptTemplate>
    fun findByMerchantIdAndId(merchantId: Long, id: Long): Optional<ReceiptTemplate>
}
