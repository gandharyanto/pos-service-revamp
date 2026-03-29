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
    val outletIds: List<Long> = emptyList(),
    val productIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val customerIds: List<Long> = emptyList()
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
    val isActive: Boolean = true,
    /** Full-replace: binding lama dihapus, diganti dengan list ini */
    val outletIds: List<Long> = emptyList(),
    val productIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val customerIds: List<Long> = emptyList()
)

data class ValidateDiscountRequest(
    val code: String,
    val outletId: Long? = null,
    val customerId: Long? = null,
    val grossSubTotal: BigDecimal
)
