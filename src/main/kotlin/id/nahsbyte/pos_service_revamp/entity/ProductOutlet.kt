package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_outlet")
class ProductOutlet : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "outlet_price")
    var outletPrice: BigDecimal? = null

    @Column(name = "stock_qty")
    var stockQty: Int = 0

    @Column(name = "is_visible")
    var isVisible: Boolean = true

    @Column(name = "can_standalone")
    var canStandalone: Boolean = true
}
