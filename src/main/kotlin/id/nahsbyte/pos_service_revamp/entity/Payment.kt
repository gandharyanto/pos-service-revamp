package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payment")
class Payment : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0

    @Column(name = "payment_trx_id")
    var paymentTrxId: String? = null

    @Column(name = "payment_method")
    var paymentMethod: String? = null

    @Column(name = "payment_source")
    var paymentSource: String? = null

    @Column(name = "amount_paid")
    var amountPaid: BigDecimal = BigDecimal.ZERO

    var status: String? = null

    @Column(name = "is_effective")
    var isEffective: Boolean = true

    @Column(name = "payment_reference")
    var paymentReference: String? = null

    @Column(name = "payment_date")
    var paymentDate: LocalDateTime? = null

    @Column(name = "payment_snapshot", columnDefinition = "text")
    var paymentSnapshot: String? = null
}
