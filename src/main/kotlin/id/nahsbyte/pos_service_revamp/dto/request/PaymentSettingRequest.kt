package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreatePaymentSettingRequest(
    val outletId: Long? = null,
    /** true = harga produk sudah include pajak (include tax), false = exclude tax */
    val isPriceIncludeTax: Boolean = false,
    val isRounding: Boolean = false,
    val roundingTarget: Int? = null,
    val roundingType: String? = null,
    val isServiceCharge: Boolean = false,
    val serviceChargePercentage: BigDecimal? = null,
    val serviceChargeAmount: BigDecimal? = null,
    /** BEFORE_TAX | AFTER_TAX | DPP | AFTER_DISCOUNT */
    val serviceChargeSource: String? = null
)

data class UpdatePaymentSettingRequest(
    val paymentSettingId: Long,
    val outletId: Long? = null,
    /** true = harga produk sudah include pajak (include tax), false = exclude tax */
    val isPriceIncludeTax: Boolean = false,
    val isRounding: Boolean = false,
    val roundingTarget: Int? = null,
    val roundingType: String? = null,
    val isServiceCharge: Boolean = false,
    val serviceChargePercentage: BigDecimal? = null,
    val serviceChargeAmount: BigDecimal? = null,
    /** BEFORE_TAX | AFTER_TAX | DPP | AFTER_DISCOUNT */
    val serviceChargeSource: String? = null
)
