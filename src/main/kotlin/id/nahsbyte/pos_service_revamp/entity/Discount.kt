package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "discount")
class Discount : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /**
     * Cara diskon dipicu:
     * TRANSACTION — diskon langsung ke total transaksi
     * ITEM_QTY    — diskon bertingkat berdasarkan qty item tertentu
     */
    var type: String = "TRANSACTION"

    /**
     * Tipe nilai diskon (untuk type=TRANSACTION):
     * PERCENTAGE | AMOUNT
     */
    @Column(name = "value_type")
    var valueType: String = "PERCENTAGE"

    /**
     * Nilai diskon (untuk type=TRANSACTION).
     * Untuk type=ITEM_QTY, nilai per tier disimpan di tabel discount_tier.
     */
    var value: BigDecimal = BigDecimal.ZERO

    /**
     * Untuk type=ITEM_QTY: produk mana yang qty-nya dihitung.
     * Null = berlaku untuk total qty semua item di transaksi.
     */
    @Column(name = "product_id")
    var productId: Long? = null

    /** Minimum purchase amount to apply discount (untuk type=TRANSACTION) */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    /** ALL_OUTLET | SPECIFIC_OUTLET */
    var visibility: String = "ALL_OUTLET"

    @Column(name = "start_date")
    var startDate: LocalDateTime? = null

    @Column(name = "end_date")
    var endDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
