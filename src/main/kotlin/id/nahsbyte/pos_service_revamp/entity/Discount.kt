package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "discount")
class Discount : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** PERCENTAGE | AMOUNT */
    var type: String = "PERCENTAGE"

    var value: BigDecimal = BigDecimal.ZERO

    /** Minimum purchase amount to apply discount */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    /** ALL_OUTLET | SPECIFIC_OUTLET */
    var visibility: String = "ALL_OUTLET"

    @Column(name = "start_date")
    var startDate: LocalDateTime? = null

    @Column(name = "end_date")
    var endDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
