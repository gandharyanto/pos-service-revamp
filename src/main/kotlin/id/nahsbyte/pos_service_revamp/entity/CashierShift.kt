package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "cashier_shift")
class CashierShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    var username: String? = null

    @Column(name = "opening_cash")
    var openingCash: BigDecimal = BigDecimal.ZERO

    @Column(name = "closing_cash")
    var closingCash: BigDecimal? = null

    @Column(name = "open_date", nullable = false)
    var openDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "close_date")
    var closeDate: LocalDateTime? = null

    /** OPEN | CLOSED */
    var status: String = "OPEN"

    var note: String? = null

    @Column(name = "opened_by")
    var openedBy: String? = null

    @Column(name = "closed_by")
    var closedBy: String? = null
}
