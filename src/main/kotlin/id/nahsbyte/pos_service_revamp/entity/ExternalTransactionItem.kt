package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "external_transaction_items")
class ExternalTransactionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "external_transaction_id", nullable = false)
    var externalTransactionId: Long = 0

    @Column(name = "product_id")
    var productId: Long? = null

    @Column(name = "product_name")
    var productName: String? = null

    var price: BigDecimal = BigDecimal.ZERO
    var qty: Int = 1

    @Column(name = "total_price")
    var totalPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "product_snapshot", columnDefinition = "text")
    var productSnapshot: String? = null

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null
}
