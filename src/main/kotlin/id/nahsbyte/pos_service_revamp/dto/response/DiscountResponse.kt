package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class DiscountResponse(
    val id: Long,
    val name: String,
    val code: String?,
    val valueType: String,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val minPurchase: BigDecimal?,
    val scope: String,
    val channel: String,
    val visibility: String,
    val usageLimit: Int?,
    val usagePerCustomer: Int?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val isActive: Boolean,
    val outlets: List<Long> = emptyList(),
    val productIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val customerIds: List<Long> = emptyList(),
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)
