package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Produk/kategori yang menjadi syarat beli pada promosi BUY_X_GET_Y.
 *
 * Untuk buy_scope=PRODUCT  → isi product_id, category_id=null
 * Untuk buy_scope=CATEGORY → isi category_id, product_id=null
 */
@Entity
@Table(name = "promotion_product")
class PromotionProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "promotion_id", nullable = false)
    var promotionId: Long = 0

    @Column(name = "product_id")
    var productId: Long? = null

    @Column(name = "category_id")
    var categoryId: Long? = null

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
