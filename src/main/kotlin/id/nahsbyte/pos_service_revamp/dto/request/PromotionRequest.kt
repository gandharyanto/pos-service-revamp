package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePromotionRequest(
    val name: String,
    /** DISCOUNT_BY_ORDER | BUY_X_GET_Y | DISCOUNT_BY_ITEM_SUBTOTAL */
    val promoType: String,
    val priority: Int = 0,
    val canCombine: Boolean = false,
    val valueType: String? = null,
    val value: BigDecimal? = null,
    val minPurchase: BigDecimal? = null,
    val buyQty: Int? = null,
    val getQty: Int? = null,
    val allowMultiple: Boolean = false,
    /** FREE | PERCENTAGE | AMOUNT | FIXED_PRICE */
    val rewardType: String? = null,
    val rewardValue: BigDecimal? = null,
    /** ALL | PRODUCT | CATEGORY */
    val buyScope: String? = null,
    /** PRODUCT | CATEGORY */
    val rewardScope: String? = null,
    val validDays: String? = null,
    /** POS | ONLINE | BOTH */
    val channel: String = "BOTH",
    /** ALL_OUTLET | SPECIFIC_OUTLET */
    val visibility: String = "ALL_OUTLET",
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    val outletIds: List<Long> = emptyList(),
    val buyProductIds: List<Long> = emptyList(),
    val buyCategoryIds: List<Long> = emptyList(),
    val rewardProductIds: List<Long> = emptyList(),
    val rewardCategoryIds: List<Long> = emptyList()
)

data class UpdatePromotionRequest(
    val id: Long,
    val name: String,
    val promoType: String,
    val priority: Int = 0,
    val canCombine: Boolean = false,
    val valueType: String? = null,
    val value: BigDecimal? = null,
    val minPurchase: BigDecimal? = null,
    val buyQty: Int? = null,
    val getQty: Int? = null,
    val allowMultiple: Boolean = false,
    val rewardType: String? = null,
    val rewardValue: BigDecimal? = null,
    val buyScope: String? = null,
    val rewardScope: String? = null,
    val validDays: String? = null,
    val channel: String = "BOTH",
    val visibility: String = "ALL_OUTLET",
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    /** Full-replace: binding lama dihapus, diganti dengan list ini */
    val outletIds: List<Long> = emptyList(),
    val buyProductIds: List<Long> = emptyList(),
    val buyCategoryIds: List<Long> = emptyList(),
    val rewardProductIds: List<Long> = emptyList(),
    val rewardCategoryIds: List<Long> = emptyList()
)
