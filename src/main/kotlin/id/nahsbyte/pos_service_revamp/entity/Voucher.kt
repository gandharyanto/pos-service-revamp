package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "voucher")
class Voucher : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(unique = true)
    var code: String = ""

    /** PERCENTAGE | AMOUNT */
    var type: String = "AMOUNT"

    var value: BigDecimal = BigDecimal.ZERO

    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    @Column(name = "max_use")
    var maxUse: Int? = null

    @Column(name = "used_count")
    var usedCount: Int = 0

    @Column(name = "expired_date")
    var expiredDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
