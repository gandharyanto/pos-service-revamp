package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "voucher_usage")
class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "voucher_id", nullable = false)
    var voucherId: Long = 0

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "customer_id")
    var customerId: Long? = null

    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "used_date")
    var usedDate: LocalDateTime = LocalDateTime.now()
}
