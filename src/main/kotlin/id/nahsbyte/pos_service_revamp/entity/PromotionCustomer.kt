package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Eligibilitas pelanggan untuk promosi tertentu.
 *
 * Jika tidak ada entry → promosi berlaku untuk semua pelanggan.
 * Jika ada entry → promosi hanya berlaku untuk pelanggan yang terdaftar di sini.
 */
@Entity
@Table(name = "promotion_customer")
class PromotionCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "promotion_id", nullable = false)
    var promotionId: Long = 0

    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
