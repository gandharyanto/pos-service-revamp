package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class LoyaltyProgramResponse(
    val id: Long,
    val name: String,
    val earnMode: String,
    val pointsPerAmount: BigDecimal,
    val earnMultiplier: BigDecimal?,
    val expiryMode: String,
    val expiryDays: Int?,
    val expiryDate: LocalDateTime?,
    val isActive: Boolean,
    val redemptionRules: List<RedemptionRuleResponse> = emptyList(),
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)

data class RedemptionRuleResponse(
    val id: Long,
    val type: String,
    // PAYMENT
    val redeemRate: BigDecimal?,
    val minRedeemPoints: BigDecimal?,
    val maxRedeemPoints: BigDecimal?,
    // DISCOUNT
    val requiredPoints: BigDecimal?,
    val discountType: String?,
    val discountValue: BigDecimal?,
    val maxDiscountAmount: BigDecimal?,
    val minPurchase: BigDecimal?,
    // FREE_PRODUCT
    val rewardProductId: Long?,
    val rewardQty: Int?,
    val isActive: Boolean
)

data class ProductLoyaltySettingResponse(
    val productId: Long,
    val isLoyaltyEnabled: Boolean,
    val fixedPoints: BigDecimal?
)
