package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Override setting loyalty per produk.
 *
 * isLoyaltyEnabled = false → produk tidak menyumbang poin
 * fixedPoints != null → produk selalu memberi nilai poin ini (abaikan global earn rate)
 * fixedPoints = null  → gunakan global earn rate
 */
@Entity
@Table(name = "product_loyalty_setting")
class ProductLoyaltySetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false, unique = true)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "is_loyalty_enabled")
    var isLoyaltyEnabled: Boolean = true

    /** Poin tetap per item. Null = ikuti global earn rate */
    @Column(name = "fixed_points")
    var fixedPoints: BigDecimal? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
}
