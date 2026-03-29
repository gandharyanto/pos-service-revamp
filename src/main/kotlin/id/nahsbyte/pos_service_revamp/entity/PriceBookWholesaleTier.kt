package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Tier harga grosir per produk — untuk price book tipe WHOLESALE.
 *
 * Contoh: produk "Kopi Susu"
 *   min_qty=1,  max_qty=2,    price=15000
 *   min_qty=3,  max_qty=5,    price=13000
 *   min_qty=6,  max_qty=null, price=11000
 *
 * Lookup: cari tier dimana min_qty <= qty_dibeli AND (max_qty IS NULL OR max_qty >= qty_dibeli)
 */
@Entity
@Table(name = "price_book_wholesale_tier")
class PriceBookWholesaleTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "price_book_id", nullable = false)
    var priceBookId: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    /** Qty minimum (inklusif) */
    @Column(name = "min_qty", nullable = false)
    var minQty: Int = 1

    /** Qty maksimum (inklusif). Null = tidak ada batas atas */
    @Column(name = "max_qty")
    var maxQty: Int? = null

    /** Harga per unit untuk tier ini */
    var price: BigDecimal = BigDecimal.ZERO

    @Column(name = "display_order")
    var displayOrder: Int? = null
}
