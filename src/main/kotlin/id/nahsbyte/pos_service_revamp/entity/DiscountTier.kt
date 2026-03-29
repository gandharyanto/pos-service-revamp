package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Tier untuk diskon tipe ITEM_QTY dan ITEM_SUBTOTAL.
 *
 * ITEM_QTY      — min_val/max_val = rentang qty (bilangan bulat disimpan sebagai BigDecimal)
 * ITEM_SUBTOTAL — min_val/max_val = rentang subtotal amount
 *
 * Contoh ITEM_QTY:
 *   min_val=1,  max_val=2,    value_type=PERCENTAGE, value=0
 *   min_val=3,  max_val=5,    value_type=PERCENTAGE, value=10
 *   min_val=6,  max_val=null, value_type=PERCENTAGE, value=20
 *
 * Contoh ITEM_SUBTOTAL:
 *   min_val=0,      max_val=49999,  value_type=PERCENTAGE, value=0
 *   min_val=50000,  max_val=99999,  value_type=PERCENTAGE, value=5
 *   min_val=100000, max_val=null,   value_type=PERCENTAGE, value=10
 */
@Entity
@Table(name = "discount_tier")
class DiscountTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    /** Nilai minimum (inklusif) — qty atau amount tergantung discount.type */
    @Column(name = "min_val", nullable = false)
    var minVal: BigDecimal = BigDecimal.ZERO

    /** Nilai maksimum (inklusif). Null = tidak ada batas atas */
    @Column(name = "max_val")
    var maxVal: BigDecimal? = null

    /** PERCENTAGE | AMOUNT */
    @Column(name = "value_type", nullable = false)
    var valueType: String = "PERCENTAGE"

    /** Nilai diskon untuk tier ini */
    var value: BigDecimal = BigDecimal.ZERO

    @Column(name = "display_order")
    var displayOrder: Int? = null
}
