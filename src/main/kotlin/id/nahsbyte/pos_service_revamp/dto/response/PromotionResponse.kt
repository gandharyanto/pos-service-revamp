package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class PromotionResponse(
    val id: Long,
    val name: String,
    val promoType: String,
    val priority: Int,
    val canCombine: Boolean,
    val valueType: String?,
    val value: BigDecimal?,
    val minPurchase: BigDecimal?,
    val buyQty: Int?,
    val getQty: Int?,
    val allowMultiple: Boolean,
    val rewardType: String?,
    val rewardValue: BigDecimal?,
    val buyScope: String?,
    val rewardScope: String?,
    val validDays: String?,
    val channel: String,
    val visibility: String,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val isActive: Boolean,
    val outlets: List<Long>,
    val buyProducts: List<Long>,
    val buyCategories: List<Long>,
    val rewardProducts: List<Long>,
    val rewardCategories: List<Long>,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)
