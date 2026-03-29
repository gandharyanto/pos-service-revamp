package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateDiscountRequest(
    val name: String,
    val code: String? = null,
    /** PERCENTAGE | AMOUNT */
    val valueType: String,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal? = null,
    val minPurchase: BigDecimal? = null,
    /** ALL | PRODUCT | CATEGORY */
    val scope: String = "ALL",
    /** POS | ONLINE | BOTH */
    val channel: String = "BOTH",
    /** ALL_OUTLET | SPECIFIC_OUTLET */
    val visibility: String = "ALL_OUTLET",
    val usageLimit: Int? = null,
    val usagePerCustomer: Int? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    /** Langsung bind outlet saat create (jika visibility=SPECIFIC_OUTLET) */
    val outletIds: List<Long> = emptyList(),
    /** Langsung bind produk saat create (jika scope=PRODUCT) */
    val productIds: List<Long> = emptyList(),
    /** Langsung bind kategori saat create (jika scope=CATEGORY) */
    val categoryIds: List<Long> = emptyList()
)

data class UpdateDiscountRequest(
    val id: Long,
    val name: String,
    val code: String? = null,
    val valueType: String,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal? = null,
    val minPurchase: BigDecimal? = null,
    val scope: String = "ALL",
    val channel: String = "BOTH",
    val visibility: String = "ALL_OUTLET",
    val usageLimit: Int? = null,
    val usagePerCustomer: Int? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val isActive: Boolean = true
)
