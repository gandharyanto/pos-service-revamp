package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Customer
import id.nahsbyte.pos_service_revamp.entity.LoyaltyProgram
import id.nahsbyte.pos_service_revamp.entity.LoyaltyRedemptionRule
import id.nahsbyte.pos_service_revamp.entity.LoyaltyTransaction
import id.nahsbyte.pos_service_revamp.entity.ProductLoyaltySetting
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CustomerRepository : JpaRepository<Customer, Long> {
    fun findByMerchantId(merchantId: Long): List<Customer>
    fun findByMerchantIdAndPhone(merchantId: Long, phone: String): Optional<Customer>
    fun findByMerchantIdAndEmail(merchantId: Long, email: String): Optional<Customer>
}

interface LoyaltyProgramRepository : JpaRepository<LoyaltyProgram, Long> {
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): Optional<LoyaltyProgram>
    fun findByMerchantId(merchantId: Long): List<LoyaltyProgram>
}

interface LoyaltyRedemptionRuleRepository : JpaRepository<LoyaltyRedemptionRule, Long> {
    fun findByLoyaltyProgramIdAndIsActiveTrue(loyaltyProgramId: Long): List<LoyaltyRedemptionRule>
    fun findByLoyaltyProgramIdAndTypeAndIsActiveTrue(loyaltyProgramId: Long, type: String): Optional<LoyaltyRedemptionRule>
    fun findByLoyaltyProgramId(loyaltyProgramId: Long): List<LoyaltyRedemptionRule>
    fun deleteAllByLoyaltyProgramId(loyaltyProgramId: Long)
}

interface ProductLoyaltySettingRepository : JpaRepository<ProductLoyaltySetting, Long> {
    fun findByProductIdAndMerchantId(productId: Long, merchantId: Long): Optional<ProductLoyaltySetting>
    fun findByMerchantIdAndProductIdIn(merchantId: Long, productIds: List<Long>): List<ProductLoyaltySetting>
}

interface LoyaltyTransactionRepository : JpaRepository<LoyaltyTransaction, Long> {
    fun findByCustomerIdOrderByCreatedDateDesc(customerId: Long): List<LoyaltyTransaction>
    fun findByTransactionId(transactionId: Long): List<LoyaltyTransaction>
}
