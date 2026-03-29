package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Promotion
import id.nahsbyte.pos_service_revamp.entity.PromotionProduct
import id.nahsbyte.pos_service_revamp.entity.PromotionRewardProduct
import id.nahsbyte.pos_service_revamp.entity.PromotionOutlet
import id.nahsbyte.pos_service_revamp.entity.PromotionCustomer
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionRepository : JpaRepository<Promotion, Long> {
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<Promotion>
}

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long> {
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionProduct>
}

interface PromotionRewardProductRepository : JpaRepository<PromotionRewardProduct, Long> {
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionRewardProduct>
}

interface PromotionOutletRepository : JpaRepository<PromotionOutlet, Long> {
    fun findByPromotionIdIn(promotionIds: List<Long>): List<PromotionOutlet>
}

interface PromotionCustomerRepository : JpaRepository<PromotionCustomer, Long> {
    fun findByPromotionIdInAndCustomerId(promotionIds: List<Long>, customerId: Long): List<PromotionCustomer>
}
