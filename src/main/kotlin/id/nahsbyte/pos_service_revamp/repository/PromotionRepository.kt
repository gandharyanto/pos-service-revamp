package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Promotion
import id.nahsbyte.pos_service_revamp.entity.PromotionProduct
import id.nahsbyte.pos_service_revamp.entity.PromotionRewardProduct
import id.nahsbyte.pos_service_revamp.entity.PromotionOutlet
import id.nahsbyte.pos_service_revamp.entity.PromotionCustomer
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionRepository : JpaRepository<Promotion, Long> {
    fun findByMerchantId(merchantId: Long): List<Promotion>
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<Promotion>
}

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long> {
    fun findByPromotionId(promotionId: Long): List<PromotionProduct>
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionProduct>
    fun deleteAllByPromotionId(promotionId: Long)
}

interface PromotionRewardProductRepository : JpaRepository<PromotionRewardProduct, Long> {
    fun findByPromotionId(promotionId: Long): List<PromotionRewardProduct>
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionRewardProduct>
    fun deleteAllByPromotionId(promotionId: Long)
}

interface PromotionOutletRepository : JpaRepository<PromotionOutlet, Long> {
    fun findByPromotionId(promotionId: Long): List<PromotionOutlet>
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionOutlet>
    fun deleteAllByPromotionId(promotionId: Long)
}

interface PromotionCustomerRepository : JpaRepository<PromotionCustomer, Long> {
    fun findByPromotionId(promotionId: Long): List<PromotionCustomer>
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionCustomer>
    fun findByPromotionIdInAndCustomerId(promotionIds: List<Long>, customerId: Long): List<PromotionCustomer>
    fun deleteAllByPromotionId(promotionId: Long)
}
