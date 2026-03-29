package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.OrderType
import org.springframework.data.jpa.repository.JpaRepository

interface OrderTypeRepository : JpaRepository<OrderType, Long> {
    fun findByMerchantId(merchantId: Long): List<OrderType>
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<OrderType>
}
