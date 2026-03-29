package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "price_book_item")
class PriceBookItem : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "price_book_id", nullable = false)
    var priceBookId: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var price: BigDecimal = BigDecimal.ZERO

    @Column(name = "is_active")
    var isActive: Boolean = true
}
