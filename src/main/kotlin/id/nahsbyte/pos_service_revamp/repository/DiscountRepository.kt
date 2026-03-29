package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Discount
import id.nahsbyte.pos_service_revamp.entity.DiscountProduct
import id.nahsbyte.pos_service_revamp.entity.DiscountCategory
import id.nahsbyte.pos_service_revamp.entity.DiscountOutlet
import id.nahsbyte.pos_service_revamp.entity.DiscountCustomer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface DiscountRepository : JpaRepository<Discount, Long> {
    fun findByCodeAndMerchantIdAndIsActiveTrue(code: String, merchantId: Long): Optional<Discount>
}

interface DiscountProductRepository : JpaRepository<DiscountProduct, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountProduct>
}

interface DiscountCategoryRepository : JpaRepository<DiscountCategory, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountCategory>
}

interface DiscountOutletRepository : JpaRepository<DiscountOutlet, Long> {
    fun findByDiscountId(discountId: Long): List<DiscountOutlet>
}

interface DiscountCustomerRepository : JpaRepository<DiscountCustomer, Long> {
    fun findByDiscountIdAndCustomerId(discountId: Long, customerId: Long): Optional<DiscountCustomer>
}
