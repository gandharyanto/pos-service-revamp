package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_archived")
class ProductArchived : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "merchant_unique_code")
    var merchantUniqueCode: String? = null

    var name: String = ""
    var price: BigDecimal = BigDecimal.ZERO
    var sku: String? = null
    var upc: String? = null

    @Column(name = "image_url", columnDefinition = "text")
    var imageUrl: String? = null

    @Column(name = "image_thumb_url", columnDefinition = "text")
    var imageThumbUrl: String? = null

    var description: String? = null

    @Column(name = "product_hash")
    var productHash: String? = null

    @Column(name = "last_stock_qty")
    var lastStockQty: Int? = null
}
