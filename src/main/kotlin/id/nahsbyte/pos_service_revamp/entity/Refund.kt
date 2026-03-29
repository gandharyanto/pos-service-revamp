package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "refund")
class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id")
    var outletId: Long? = null

    var amount: BigDecimal = BigDecimal.ZERO

    var reason: String? = null

    /** FULL | PARTIAL */
    var type: String = "FULL"

    /** PENDING | APPROVED | REJECTED */
    var status: String = "PENDING"

    @Column(name = "refund_by")
    var refundBy: String? = null

    @Column(name = "approved_by")
    var approvedBy: String? = null

    @Column(name = "refund_date")
    var refundDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "approved_date")
    var approvedDate: LocalDateTime? = null

    var note: String? = null
}
