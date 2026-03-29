package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Promosi otomatis — diterapkan tanpa input kode saat kondisi cart terpenuhi.
 *
 * Tipe promosi (promo_type):
 *   DISCOUNT_BY_ORDER         — diskon ke total transaksi jika memenuhi min_purchase
 *   BUY_X_GET_Y               — beli X item, dapatkan reward Y
 *   DISCOUNT_BY_ITEM_SUBTOTAL — diskon ke item tertentu jika subtotal item memenuhi threshold
 *                               (buyScope + promotion_product menentukan item yang dihitung,
 *                                min_purchase = threshold subtotal, value/valueType = nilai diskon)
 *
 * Buy scope / Reward scope:
 *   ALL      — berlaku untuk semua produk
 *   PRODUCT  — produk spesifik (lihat tabel promotion_product / promotion_reward_product)
 *   CATEGORY — kategori spesifik (lihat tabel promotion_product / promotion_reward_product)
 *
 * Reward type (untuk BUY_X_GET_Y):
 *   FREE         — item reward gratis
 *   PERCENTAGE   — item reward didiskon sekian persen
 *   AMOUNT       — item reward didiskon sekian rupiah
 *   FIXED_PRICE  — item reward dijual pada harga tetap
 *
 * Channel:
 *   POS | ONLINE | BOTH
 *
 * Valid days (comma-separated):
 *   MON,TUE,WED,THU,FRI,SAT,SUN — null = berlaku setiap hari
 */
@Entity
@Table(name = "promotion")
class Promotion : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** DISCOUNT_BY_ORDER | BUY_X_GET_Y */
    @Column(name = "promo_type", nullable = false)
    var promoType: String = "DISCOUNT_BY_ORDER"

    /**
     * Urutan prioritas — angka lebih kecil = lebih prioritas.
     * Digunakan saat beberapa promosi aktif bersamaan.
     */
    var priority: Int = 0

    /** Boleh digabung dengan promosi lain dalam satu transaksi */
    @Column(name = "can_combine")
    var canCombine: Boolean = false

    // --- DISCOUNT_BY_ORDER fields ---

    /** PERCENTAGE | AMOUNT */
    @Column(name = "value_type")
    var valueType: String? = null

    var value: BigDecimal? = null

    /** Minimum total transaksi untuk trigger promosi */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    // --- BUY_X_GET_Y fields ---

    /** Jumlah item yang harus dibeli */
    @Column(name = "buy_qty")
    var buyQty: Int? = null

    /** Jumlah item reward yang didapat */
    @Column(name = "get_qty")
    var getQty: Int? = null

    /**
     * Jika true: reward berlipat sesuai kelipatan buy_qty.
     * Contoh: buy_qty=2, get_qty=1, allow_multiple=true
     * → beli 4 dapat 2, beli 6 dapat 3
     */
    @Column(name = "allow_multiple")
    var allowMultiple: Boolean = false

    /** FREE | PERCENTAGE | AMOUNT | FIXED_PRICE */
    @Column(name = "reward_type")
    var rewardType: String? = null

    /** Nilai reward (untuk rewardType PERCENTAGE/AMOUNT/FIXED_PRICE) */
    @Column(name = "reward_value")
    var rewardValue: BigDecimal? = null

    /** Scope produk yang dihitung untuk syarat beli: ALL | PRODUCT | CATEGORY */
    @Column(name = "buy_scope")
    var buyScope: String? = null

    /** Scope produk reward: PRODUCT | CATEGORY */
    @Column(name = "reward_scope")
    var rewardScope: String? = null

    // --- Validity ---

    /** Hari berlaku, comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN. Null = setiap hari */
    @Column(name = "valid_days")
    var validDays: String? = null

    /** POS | ONLINE | BOTH */
    var channel: String = "BOTH"

    /** ALL_OUTLET | SPECIFIC_OUTLET */
    var visibility: String = "ALL_OUTLET"

    @Column(name = "start_date")
    var startDate: LocalDateTime? = null

    @Column(name = "end_date")
    var endDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
