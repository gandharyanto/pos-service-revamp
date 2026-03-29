package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Merchant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MerchantRepository : JpaRepository<Merchant, Long> {
    fun findByMerchantUniqueCode(code: String): Optional<Merchant>
}
