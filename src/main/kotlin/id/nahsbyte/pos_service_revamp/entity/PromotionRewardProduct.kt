package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Produk/kategori yang menjadi reward pada promosi BUY_X_GET_Y.
 *
 * Untuk reward_scope=PRODUCT  → isi product_id, category_id=null
 * Untuk reward_scope=CATEGORY → isi category_id, product_id=null
 */
@Entity
@Table(name = "promotion_reward_product")
class PromotionRewardProduct {
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
