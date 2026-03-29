package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock_movement")
class StockMovement : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id")
    var outletId: Long? = null

    @Column(name = "reference_id")
    var referenceId: Long? = null

    var qty: Int = 0

    @Column(name = "movement_type")
    var movementType: String = ""

    @Column(name = "movement_reason")
    var movementReason: String? = null

    @Column(columnDefinition = "text")
    var note: String? = null
}
