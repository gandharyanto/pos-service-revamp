package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateLoyaltyProgramRequest(
    val name: String,
    /** RATIO | MULTIPLY */
    val earnMode: String = "RATIO",
    val pointsPerAmount: BigDecimal = BigDecimal.ZERO,
    val earnMultiplier: BigDecimal? = null,
    /** NONE | ROLLING_DAYS | FIXED_DATE */
    val expiryMode: String = "NONE",
    val expiryDays: Int? = null,
    val expiryDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    val rules: List<CreateRedemptionRuleRequest> = emptyList()
)

data class UpdateLoyaltyProgramRequest(
    val id: Long,
    val name: String,
    val earnMode: String = "RATIO",
    val pointsPerAmount: BigDecimal = BigDecimal.ZERO,
    val earnMultiplier: BigDecimal? = null,
    val expiryMode: String = "NONE",
    val expiryDays: Int? = null,
    val expiryDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    /** Full-replace: rules lama dihapus, diganti dengan list ini */
    val rules: List<CreateRedemptionRuleRequest> = emptyList()
)

data class CreateRedemptionRuleRequest(
    /** PAYMENT | DISCOUNT | FREE_PRODUCT */
    val type: String,
    // PAYMENT
    val redeemRate: BigDecimal? = null,
    val minRedeemPoints: BigDecimal? = null,
    val maxRedeemPoints: BigDecimal? = null,
    // DISCOUNT
    val requiredPoints: BigDecimal? = null,
    val discountType: String? = null,
    val discountValue: BigDecimal? = null,
    val maxDiscountAmount: BigDecimal? = null,
    val minPurchase: BigDecimal? = null,
    // FREE_PRODUCT
    val rewardProductId: Long? = null,
    val rewardQty: Int? = null,
    val isActive: Boolean = true
)

data class SetProductLoyaltyRequest(
    val productId: Long,
    val isLoyaltyEnabled: Boolean = true,
    val fixedPoints: BigDecimal? = null
)
