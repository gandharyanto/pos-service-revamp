package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Aturan penukaran poin loyalti per merchant.
 *
 * Type:
 *   PAYMENT      — poin sebagai alat bayar (redeemRate: 1 poin = Rp X)
 *   DISCOUNT     — poin ditukar diskon (requiredPoints → discountValue)
 *   FREE_PRODUCT — poin ditukar produk gratis
 *
 * Satu program bisa punya beberapa rule, tapi hanya satu yang aktif per type.
 */
@Entity
@Table(name = "loyalty_redemption_rule")
class LoyaltyRedemptionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "loyalty_program_id", nullable = false)
    var loyaltyProgramId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    /** PAYMENT | DISCOUNT | FREE_PRODUCT */
    var type: String = "PAYMENT"

    // ── PAYMENT fields ───────────────────────────────────────────────
    /** 1 poin = Rp X */
    @Column(name = "redeem_rate")
    var redeemRate: BigDecimal? = null

    /** Minimum poin yang bisa diredeem sekaligus */
    @Column(name = "min_redeem_points")
    var minRedeemPoints: BigDecimal? = null

    /** Maksimum poin per transaksi. Null = tidak terbatas */
    @Column(name = "max_redeem_points")
    var maxRedeemPoints: BigDecimal? = null

    // ── DISCOUNT fields ──────────────────────────────────────────────
    /** Jumlah poin yang harus ditukarkan */
    @Column(name = "required_points")
    var requiredPoints: BigDecimal? = null

    /** PERCENTAGE | AMOUNT */
    @Column(name = "discount_type")
    var discountType: String? = null

    /** Nilai diskon */
    @Column(name = "discount_value")
    var discountValue: BigDecimal? = null

    /** Cap maksimum diskon (untuk discountType=PERCENTAGE). Null = tidak ada cap */
    @Column(name = "max_discount_amount")
    var maxDiscountAmount: BigDecimal? = null

    /** Minimum total transaksi agar bisa redeem */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    // ── FREE_PRODUCT fields ──────────────────────────────────────────
    @Column(name = "reward_product_id")
    var rewardProductId: Long? = null

    @Column(name = "reward_qty")
    var rewardQty: Int? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "created_date")
    var createdDate: java.time.LocalDateTime? = null
}
