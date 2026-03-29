package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Konfigurasi program loyalty per merchant.
 *
 * Earn mode:
 *   RATIO    — setiap Rp [pointsPerAmount] = 1 poin  (floor(total / pointsPerAmount))
 *   MULTIPLY — total × [earnMultiplier] = poin         (floor(total × earnMultiplier))
 *
 * Expiry mode:
 *   NONE         — poin tidak kadaluarsa
 *   ROLLING_DAYS — poin kadaluarsa N hari setelah diperoleh
 *   FIXED_DATE   — poin kadaluarsa pada tanggal tetap
 */
@Entity
@Table(name = "loyalty_program")
class LoyaltyProgram : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** RATIO | MULTIPLY */
    @Column(name = "earn_mode")
    var earnMode: String = "RATIO"

    /** Untuk RATIO: setiap Rp X mendapat 1 poin */
    @Column(name = "points_per_amount")
    var pointsPerAmount: BigDecimal = BigDecimal.ZERO

    /** Untuk MULTIPLY: total × multiplier = poin */
    @Column(name = "earn_multiplier")
    var earnMultiplier: BigDecimal? = null

    /** NONE | ROLLING_DAYS | FIXED_DATE */
    @Column(name = "expiry_mode")
    var expiryMode: String = "NONE"

    /** Untuk ROLLING_DAYS: poin kadaluarsa N hari setelah diperoleh */
    @Column(name = "expiry_days")
    var expiryDays: Int? = null

    /** Untuk FIXED_DATE: tanggal kadaluarsa tetap */
    @Column(name = "expiry_date")
    var expiryDate: LocalDateTime? = null

    /** @deprecated Dipindahkan ke LoyaltyRedemptionRule. Dipertahankan untuk backward compat. */
    @Column(name = "redeem_rate")
    var redeemRate: BigDecimal = BigDecimal.ZERO

    /** @deprecated Dipindahkan ke LoyaltyRedemptionRule. Dipertahankan untuk backward compat. */
    @Column(name = "min_redeem_points")
    var minRedeemPoints: BigDecimal? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
