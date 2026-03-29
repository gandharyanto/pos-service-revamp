package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal

data class PaymentSettingResponse(
    val paymentSettingId: Long,
    val isPriceIncludeTax: Boolean,
    val isRounding: Boolean,
    val roundingTarget: Int?,
    val roundingType: String?,
    val isServiceCharge: Boolean,
    val serviceChargePercentage: BigDecimal?,
    val serviceChargeAmount: BigDecimal?,
    val isTax: Boolean,
    val taxPercentage: BigDecimal?,
    val taxName: String?
)

data class PaymentMethodResponse(
    val code: String,
    val name: String,
    val category: String?,
    val paymentType: String?,
    val provider: String?
)

data class PaymentMethodListResponse(
    val internalPayments: List<PaymentMethodResponse>,
    val externalPayments: List<PaymentMethodResponse>
)
