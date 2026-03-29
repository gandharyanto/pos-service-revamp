package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "loyalty_transaction")
class LoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "customer_id", nullable = false)
    var customerId: Long = 0

    @Column(name = "transaction_id")
    var transactionId: Long? = null

    var points: BigDecimal = BigDecimal.ZERO

    /**
     * EARN             — poin diperoleh dari transaksi
     * REDEEM_PAYMENT   — poin ditukarkan sebagai alat bayar
     * REDEEM_DISCOUNT  — poin ditukarkan sebagai diskon
     * REDEEM_PRODUCT   — poin ditukarkan sebagai produk gratis
     * EXPIRE           — poin kadaluarsa
     * ADJUST           — penyesuaian manual oleh merchant
     *
     * Legacy: GET | REDEEM | ADJUSTMENT (dipertahankan untuk data lama)
     */
    var type: String = "EARN"

    var note: String? = null

    /** Kapan poin ini kadaluarsa (untuk expiry mode ROLLING_DAYS) */
    @Column(name = "expiry_date")
    var expiryDate: LocalDateTime? = null

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null
}
