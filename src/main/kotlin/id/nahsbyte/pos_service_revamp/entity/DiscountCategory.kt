package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Binding kategori ke discount code (untuk scope=CATEGORY).
 * Satu discount bisa berlaku untuk satu atau lebih kategori.
 */
@Entity
@Table(name = "discount_category")
class DiscountCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "category_id", nullable = false)
    var categoryId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
