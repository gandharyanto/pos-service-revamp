package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "transaction")
class Transaction : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "merchant_unique_code")
    var merchantUniqueCode: String? = null

    var username: String? = null

    @Column(name = "trx_id")
    var trxId: String? = null

    @Column(name = "transaction_origin")
    var transactionOrigin: String? = null

    var status: String = ""

    @Column(name = "payment_method")
    var paymentMethod: String? = null

    @Column(name = "price_include_tax")
    var priceIncludeTax: Boolean = false

    @Column(name = "sub_total")
    var subTotal: BigDecimal = BigDecimal.ZERO

    @Column(name = "total_amount")
    var totalAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "service_charge_percentage")
    var serviceChargePercentage: BigDecimal? = null

    @Column(name = "service_charge_amount")
    var serviceChargeAmount: BigDecimal? = null

    @Column(name = "total_service_charge")
    var totalServiceCharge: BigDecimal? = null

    @Column(name = "tax_percentage")
    var taxPercentage: BigDecimal? = null

    @Column(name = "total_tax")
    var totalTax: BigDecimal? = null

    @Column(name = "tax_name")
    var taxName: String? = null

    @Column(name = "total_rounding")
    var totalRounding: BigDecimal? = null

    @Column(name = "rounding_type")
    var roundingType: String? = null

    @Column(name = "rounding_target")
    var roundingTarget: String? = null

    @Column(name = "cash_tendered")
    var cashTendered: BigDecimal? = null

    @Column(name = "cash_change")
    var cashChange: BigDecimal? = null

    @Column(name = "queue_id")
    var queueId: Long? = null
}
