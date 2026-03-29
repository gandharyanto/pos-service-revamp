package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "transaction_queue")
class TransactionQueue : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "queue_number")
    var queueNumber: String = ""

    @Column(name = "queue_date")
    var queueDate: LocalDate? = null

    var status: String = ""
}
