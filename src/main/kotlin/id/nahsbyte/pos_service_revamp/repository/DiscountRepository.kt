package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Discount
import id.nahsbyte.pos_service_revamp.entity.DiscountProduct
import id.nahsbyte.pos_service_revamp.entity.DiscountCategory
import id.nahsbyte.pos_service_revamp.entity.DiscountOutlet
import id.nahsbyte.pos_service_revamp.entity.DiscountCustomer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface DiscountRepository : JpaRepository<Discount, Long> {
    fun findByMerchantId(merchantId: Long): List<Discount>
    fun findByCodeAndMerchantIdAndIsActiveTrue(code: String, merchantId: Long): Optional<Discount>
}

interface DiscountProductRepository : JpaRepository<DiscountProduct, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountProduct>
    fun findByDiscountIdIn(discountIds: List<Long>): List<DiscountProduct>
    fun deleteAllByDiscountId(discountId: Long)
}

interface DiscountCategoryRepository : JpaRepository<DiscountCategory, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountCategory>
    fun findByDiscountIdIn(discountIds: List<Long>): List<DiscountCategory>
    fun deleteAllByDiscountId(discountId: Long)
}

interface DiscountOutletRepository : JpaRepository<DiscountOutlet, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountOutlet>
    fun findByDiscountIdIn(discountIds: List<Long>): List<DiscountOutlet>
    fun deleteAllByDiscountId(discountId: Long)
}

interface DiscountCustomerRepository : JpaRepository<DiscountCustomer, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountCustomer>
    fun findByDiscountIdIn(discountIds: List<Long>): List<DiscountCustomer>
    fun findByDiscountIdAndCustomerId(discountId: Long, customerId: Long): Optional<DiscountCustomer>
    fun deleteAllByDiscountId(discountId: Long)
}
