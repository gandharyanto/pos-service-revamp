package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Tax
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TaxRepository : JpaRepository<Tax, Long> {
    fun findAllByMerchantIdAndIsActiveTrue(merchantId: Long): List<Tax>
    fun findByMerchantIdAndIsDefaultTrue(merchantId: Long): Optional<Tax>
}
