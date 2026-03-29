package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Tier untuk diskon tipe ITEM_QTY.
 * Setiap tier mendefinisikan rentang qty dan nilai diskon yang berlaku.
 *
 * Contoh:
 *   min_qty=1,  max_qty=2,    value_type=PERCENTAGE, value=0
 *   min_qty=3,  max_qty=5,    value_type=PERCENTAGE, value=10
 *   min_qty=6,  max_qty=null, value_type=PERCENTAGE, value=20
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

    /** Qty minimum (inklusif) untuk tier ini berlaku */
    @Column(name = "min_qty", nullable = false)
    var minQty: Int = 1

    /** Qty maksimum (inklusif). Null = tidak ada batas atas */
    @Column(name = "max_qty")
    var maxQty: Int? = null

    /** PERCENTAGE | AMOUNT */
    @Column(name = "value_type", nullable = false)
    var valueType: String = "PERCENTAGE"

    /** Nilai diskon untuk tier ini */
    var value: BigDecimal = BigDecimal.ZERO

    @Column(name = "display_order")
    var displayOrder: Int? = null
}
