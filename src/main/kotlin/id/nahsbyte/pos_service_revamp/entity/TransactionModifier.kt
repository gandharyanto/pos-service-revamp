package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "transaction_modifier")
class TransactionModifier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "transaction_item_id", nullable = false)
    var transactionItemId: Long = 0

    @Column(name = "modifier_id")
    var modifierId: Long? = null

    /** Snapshot nama modifier saat transaksi */
    @Column(name = "modifier_name")
    var modifierName: String? = null

    /** Snapshot harga modifier saat transaksi */
    @Column(name = "additional_price")
    var additionalPrice: BigDecimal = BigDecimal.ZERO

    var qty: Int = 1

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "created_date")
    var createdDate: java.time.LocalDateTime? = null
}
