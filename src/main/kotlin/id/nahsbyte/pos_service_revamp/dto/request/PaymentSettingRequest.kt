package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreatePaymentSettingRequest(
    val isPriceIncludeTax: Boolean = false,
    val isRounding: Boolean = false,
    val roundingTarget: Int? = null,
    val roundingType: String? = null,
    val isServiceCharge: Boolean = false,
    val serviceChargePercentage: BigDecimal? = null,
    val serviceChargeAmount: BigDecimal? = null,
    val isTax: Boolean = false,
    val taxPercentage: BigDecimal? = null,
    val taxName: String? = null
)

data class UpdatePaymentSettingRequest(
    val paymentSettingId: Long,
    val isPriceIncludeTax: Boolean = false,
    val isRounding: Boolean = false,
    val roundingTarget: Int? = null,
    val roundingType: String? = null,
    val isServiceCharge: Boolean = false,
    val serviceChargePercentage: BigDecimal? = null,
    val serviceChargeAmount: BigDecimal? = null,
    val isTax: Boolean = false,
    val taxPercentage: BigDecimal? = null,
    val taxName: String? = null
)
