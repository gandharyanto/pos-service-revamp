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
     * TRANSACTION   — diskon flat ke total transaksi
     * ITEM_QTY      — diskon bertingkat berdasarkan qty item (via discount_tier)
     * ITEM_SUBTOTAL — diskon bertingkat berdasarkan subtotal item (via discount_tier)
     */
    var type: String = "TRANSACTION"

    /**
     * Cara diskon diaplikasikan:
     * AUTO   — otomatis jika kondisi terpenuhi
     * MANUAL — kasir pilih manual
     */
    @Column(name = "apply_mode", nullable = false)
    var applyMode: String = "MANUAL"

    /**
     * Boleh digabung dengan diskon lain dalam satu transaksi.
     * false = hanya satu diskon yang berlaku (exclusive)
     */
    var stackable: Boolean = false

    /**
     * Tipe nilai diskon (untuk type=TRANSACTION):
     * PERCENTAGE | AMOUNT
     */
    @Column(name = "value_type")
    var valueType: String = "PERCENTAGE"

    /**
     * Nilai diskon (untuk type=TRANSACTION, flat).
     * Untuk ITEM_QTY dan ITEM_SUBTOTAL, nilai per tier di tabel discount_tier.
     */
    var value: BigDecimal = BigDecimal.ZERO

    /**
     * Trigger untuk AUTO+TRANSACTION: minimum total transaksi.
     * Null = tidak ada minimum.
     */
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
