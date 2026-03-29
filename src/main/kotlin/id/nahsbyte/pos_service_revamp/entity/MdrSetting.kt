package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "mdr_setting")
class MdrSetting : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "payment_method_code", nullable = false)
    var paymentMethodCode: String = ""

    /** Persentase MDR */
    var percentage: BigDecimal = BigDecimal.ZERO

    /** Biaya tetap per transaksi */
    @Column(name = "flat_fee")
    var flatFee: BigDecimal = BigDecimal.ZERO

    /** MERCHANT | CUSTOMER | SPLIT */
    @Column(name = "charged_to")
    var chargedTo: String = "MERCHANT"

    @Column(name = "is_active")
    var isActive: Boolean = true
}
