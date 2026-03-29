package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "loyalty_transaction")
class LoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0

    @Column(name = "transaction_id")
    var transactionId: Long? = null

    var points: BigDecimal = BigDecimal.ZERO

    /** GET | REDEEM | ADJUSTMENT */
    var type: String = "GET"

    var note: String? = null

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null
}
