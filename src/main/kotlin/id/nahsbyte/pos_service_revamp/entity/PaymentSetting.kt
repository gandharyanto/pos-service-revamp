package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "payment_setting")
class PaymentSetting : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "is_price_include_tax")
    var isPriceIncludeTax: Boolean = false

    @Column(name = "is_rounding")
    var isRounding: Boolean = false

    @Column(name = "rounding_target")
    var roundingTarget: Int? = null

    @Column(name = "rounding_type")
    var roundingType: String? = null

    @Column(name = "is_service_charge")
    var isServiceCharge: Boolean = false

    @Column(name = "service_charge_percentage")
    var serviceChargePercentage: BigDecimal? = null

    @Column(name = "service_charge_amount")
    var serviceChargeAmount: BigDecimal? = null

    @Column(name = "is_tax")
    var isTax: Boolean = false

    @Column(name = "tax_percentage")
    var taxPercentage: BigDecimal? = null

    @Column(name = "tax_name")
    var taxName: String? = null

    @Column(name = "tax_mode")
    var taxMode: String? = null

    // --- Columns added for revamp features ---

    /** Null = berlaku untuk semua outlet merchant ini */
    @Column(name = "outlet_id")
    var outletId: Long? = null

    /** BEFORE_TAX | AFTER_TAX | DPP | AFTER_DISCOUNT */
    @Column(name = "service_charge_source")
    var serviceChargeSource: String? = null
}
