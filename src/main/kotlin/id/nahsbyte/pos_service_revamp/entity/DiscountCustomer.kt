package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Eligibilitas pelanggan untuk discount code.
 *
 * Jika tidak ada entry di tabel ini → discount berlaku untuk semua pelanggan.
 * Jika ada entry → discount hanya berlaku untuk pelanggan yang terdaftar di sini.
 */
@Entity
@Table(name = "discount_customer")
class DiscountCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
