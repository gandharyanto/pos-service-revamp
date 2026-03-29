package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "disbursement_log")
class DisbursementLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0

    @Column(name = "rule_id", nullable = false)
    var ruleId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "recipient_id")
    var recipientId: Long? = null

    @Column(name = "recipient_name")
    var recipientName: String? = null

    var layer: String = ""

    @Column(name = "base_amount")
    var baseAmount: BigDecimal = BigDecimal.ZERO

    var percentage: BigDecimal = BigDecimal.ZERO
    var amount: BigDecimal = BigDecimal.ZERO

    /** PENDING | SETTLED | FAILED */
    var status: String = "PENDING"

    @Column(name = "created_date")
    var createdDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "settled_date")
    var settledDate: LocalDateTime? = null
}
